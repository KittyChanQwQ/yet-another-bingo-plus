package me.jfenn.bingo.common.ready

import me.jfenn.bingo.common.KEYBIND_OPEN_CARD
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.event.packet.ServerPacketEvents
import me.jfenn.bingo.common.game.GameResumeService
import me.jfenn.bingo.common.game.GameService
import me.jfenn.bingo.common.reset.ResetService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatHHMMSS
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.integrations.permissions.IPermissionsApi
import me.jfenn.bingo.platform.IExecutors
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.IPlayerManager
import org.slf4j.Logger
import java.time.Duration

internal class ReadyController(
    private val log: Logger,
    private val readyState: ReadyTimerState,
    private val state: BingoState,
    private val config: BingoConfig,
    private val text: TextProvider,
    private val gameService: GameService,
    private val playerManager: IPlayerManager,
    private val resetService: ResetService,
    private val resumeService: GameResumeService,
    private val packets: ServerPacketEvents,
    private val permissions: IPermissionsApi,
    private val readyService: ReadyService,
    private val serverTaskExecutor: IExecutors.IServerTaskExecutor,
    events: ScopedEvents,
) {
    private fun isReadyKeybindSupported(player: IPlayerHandle) =
        packets.readyUpdateV1.isSupported(player)
            || packets.readyUpdateV2.isSupported(player)

    private fun createVoteOption(type: VoteType, isSelected: Boolean) = VoteOption(
        id = type.id,
        title = when (type) {
            VoteType.START -> text.string(StringKey.GameStart)
            VoteType.KEEP_PLAYING -> resumeService.getResumeDescription()
            VoteType.NEXT_ROUND -> text.string(StringKey.WorldNextRound)
            VoteType.RETURN_TO_LOBBY -> text.string(StringKey.WorldReturnToLobby)
        },
        votes = readyState.countVotes(type),
        isSelected = isSelected
    )

    private fun createVoteOptions(player: IPlayerHandle): List<VoteOption> {
        val vote = readyState.getVote(player.uuid)
        val operatorVote = readyState.operatorVote
        val isOperator = permissions.hasPermission(player, Permission.CONFIGURE_GAME)
        return when (state.state) {
            GameState.POSTGAME -> listOf(VoteType.NEXT_ROUND, VoteType.KEEP_PLAYING, VoteType.RETURN_TO_LOBBY)
            else -> emptyList()
        }
            // if operatorVote is set, limit the non-operator choices to the chosen vote
            .filter { operatorVote == null || it == operatorVote || isOperator }
            .map {
                createVoteOption(
                    type = it,
                    isSelected = vote == it,
                )
            }
    }

    private fun update(
        playersToUpdate: List<IPlayerHandle> = playerManager.getPlayers(),
    ) {
        if (!state.isLobbyMode) return
        // Only send update packets during these gamestates
        // (note that SpawnPreloadingController also sends ReadyUpdatePacket, so this must avoid conflicts)
        if (state.state !in arrayOf(GameState.PREGAME, GameState.POSTGAME))
            return

        val isVotingEnabled = when (state.state) {
            GameState.PREGAME -> config.startWhenReadySeconds != null
            GameState.POSTGAME -> config.nextRoundWhenReadySeconds != null
            else -> false
        }

        readyState.updatePlayers(playerManager.getPlayers().filter { readyService.canUseReady(it) })
        val remainingTime = readyState.remainingTime()
        val isComplete = remainingTime?.let { it <= Duration.ZERO } ?: false

        readyService.tick()

        if (isComplete) {
            val vote = readyService.currentVote()
            log.info("[ReadyController] Reached the end of the ${state.state} timer - running ${vote}!")
            when (vote) {
                VoteType.START -> {
                    readyService.reset()
                    gameService.start(allowSpectators = true)
                }
                VoteType.RETURN_TO_LOBBY -> {
                    serverTaskExecutor.execute {
                        resetService.resetGame()
                    }
                }
                VoteType.KEEP_PLAYING -> {
                    serverTaskExecutor.execute {
                        resumeService.resume()
                    }
                }
                VoteType.NEXT_ROUND -> {
                    serverTaskExecutor.execute {
                        resetService.resetGame(GameState.STARTING_NEXT_ROUND)
                        gameService.start(ignoreWarnings = true, allowSpectators = true)
                    }
                }
                null -> {}
            }
        }

        val updatePacket = ReadyUpdatePacket(
            isRunning = readyState.isRunning && !isComplete,
            isReady = false,
            state = state.state,
            remainingDuration = readyState.remainingTime() ?: Duration.ZERO,
            totalDuration = readyState.totalTime() ?: Duration.ZERO,
            readyPlayers = playerManager.getPlayers().count { readyState.isReady(it.uuid) },
            totalPlayers = playerManager.getPlayers().size,
            title = null,
            subtitle = null,
            votePending = readyService.currentVote()?.id,
        )

        for (player in playersToUpdate) {
            val isPlayerReady = readyState.isReady(player.uuid)
            val textReadyUp = when {
                isPlayerReady -> text.string(StringKey.LobbyVotePlayersReady, updatePacket.readyPlayers, updatePacket.totalPlayers)
                readyService.canUseReady(player) -> when {
                    isReadyKeybindSupported(player) -> text.string(
                        StringKey.LobbyVoteReadyUpKeybind,
                        text.keybind("key.sneak"), text.keybind(KEYBIND_OPEN_CARD)
                    )
                    else -> text.string(StringKey.LobbyVoteReadyUpCommand, ReadyCommand.READY)
                }
                else -> null
            }
            val textTimeRemaining = readyService.currentVote()?.let {
                text.string(it.timeRemainingKey, updatePacket.remainingDuration.formatHHMMSS())
            }

            val packet = updatePacket.copy(
                isReady = isPlayerReady,
                title = textReadyUp ?: text.empty(),
                subtitle = textTimeRemaining,
                canSendReady = isVotingEnabled && permissions.hasPermission(player, Permission.COMMAND_READY),
                voteOptions = createVoteOptions(player),
            )

            when {
                packets.readyUpdateV2.send(player, packet) -> {}
                packets.readyUpdateV1.send(player, packet) -> {}
                packet.isRunning -> {
                    val message = text.joinText(
                        listOfNotNull(textTimeRemaining, textReadyUp),
                        text.literal(" - ")
                    )
                    player.sendHotbarMessage(message)
                }
            }
        }
    }

    init {
        events.onStateChange {
            readyService.reset()

            // Run an update check immediately
            update()

            // If the timer is not running, send an extra packet to dismiss the ready GUI
            if (!readyState.isRunning && it.to != GameState.PRELOADING) {
                val basePacket = ReadyUpdatePacket(
                    isRunning = false,
                    isReady = false,
                    state = state.state,
                    remainingDuration = Duration.ZERO,
                    totalDuration = Duration.ZERO,
                    readyPlayers = 0,
                    totalPlayers = 0,
                    title = null,
                    subtitle = null,
                    votePending = readyService.currentVote()?.id,
                )
                for (player in playerManager.getPlayers()) {
                    val packet = basePacket.copy(voteOptions = createVoteOptions(player))
                    when {
                        packets.readyUpdateV2.send(player, packet) -> {}
                        packets.readyUpdateV1.send(player, packet) -> {}
                    }
                }
            }
        }

        events.onChangeOptions {
            if (readyState.isRunning) {
                log.info("[ReadyController] Reset the timer, as the game options have changed")
            }
            readyService.reset()
        }

        events.onChangeTeam {
            // If the player leaves their team, un-ready them
            if (it.team == null) {
                readyState.setReady(it.player.uuid, null)
                playerManager.updatePlayerListName(it.player)
            }
            // If the player changes team while ready, update the tab name
            if (readyState.isReady(it.player.uuid)) {
                playerManager.updatePlayerListName(it.player)
            }
        }

        events.onPacket(packets.readySetV1) {
            val isReady = it.packet.isReady
            val result = readyService.setReady(it.player, isReady)
            // Only send the chat message if the player is in multiplayer (or if it failed)
            if (!state.isSingleplayer() || result !is ReadyResult.ChangedTo) {
                it.player.sendMessage(result.message)
            }
        }

        events.onPacket(packets.readySetV2) { (player, packet) ->
            val voteType = VoteType.entries.find { it.id == packet.optionId }
                ?.takeIf { packet.isReady }
                ?: (if (packet.isReady) readyService.currentVote() else null)

            val result = readyService.setReady(player, voteType)
            if (result !is ReadyResult.ChangedTo) {
                player.sendMessage(result.message)
            }
        }

        events.onPlayerChannelRegister {
            update(listOf(it.player))
        }

        events.onUpdateTick {
            update()
        }
    }
}