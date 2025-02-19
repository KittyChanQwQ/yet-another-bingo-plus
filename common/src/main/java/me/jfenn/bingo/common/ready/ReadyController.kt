package me.jfenn.bingo.common.ready

import me.jfenn.bingo.client.platform.text.IText
import me.jfenn.bingo.common.KEYBIND_OPEN_CARD
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.autorestart.ResetService
import me.jfenn.bingo.common.commands.formatWarning
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.event.packet.ServerPacketEvents
import me.jfenn.bingo.common.game.GameService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatHHMMSS
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.integrations.permissions.IPermissionsApi
import me.jfenn.bingo.platform.IExecutors
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.IPlayerManager
import net.minecraft.text.Text
import org.slf4j.Logger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class ReadyController(
    private val log: Logger,
    private val readyState: ReadyTimerState,
    private val state: BingoState,
    private val config: BingoConfig,
    private val text: TextProvider,
    private val gameService: GameService,
    private val playerManager: IPlayerManager,
    private val teamService: TeamService,
    private val resetService: ResetService,
    private val packets: ServerPacketEvents,
    private val permissions: IPermissionsApi,
    private val readyService: ReadyService,
    private val serverTaskExecutor: IExecutors.IServerTaskExecutor,
    events: ScopedEvents,
) {

    private fun isPregameReady(): Boolean {
        val players = playerManager.getPlayers()
        val isReady = players.all { teamService.isPlaying(it) } || players.any { readyState.isReady(it.uuid) }
        return isReady && players.size > 1 && state.options.isValid()
    }

    private fun reset() {
        readyState.reset()
        playerManager.getPlayers().forEach { player ->
            playerManager.updatePlayerListName(player)
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

        readyState.updatePlayers(playerManager.getPlayers())
        val remainingTime = readyState.remainingTime()
        val isComplete = remainingTime?.let { it <= Duration.ZERO } ?: false

        if (state.state == GameState.PREGAME) {
            // If the game can start, begin the timer!
            if (!readyState.isRunning && isPregameReady()) {
                config.startWhenReadySeconds?.let {
                    log.info("[ReadyController] Starting the PREGAME timer for $it seconds")
                    readyState.startTimer(it.seconds)
                }
            }

            // If the game is no longer ready, reset the timer!
            if (readyState.isRunning && !isPregameReady()) {
                log.info("[ReadyController] Cancelling the PREGAME timer, as players are not ready")
                reset()
            }

            if (isComplete) {
                log.info("[ReadyController] Reached the end of the PREGAME timer - starting the game!")
                reset()
                val warnings = mutableListOf<IText>()
                gameService.start(warnings, allowSpectators = true)
                warnings.forEachIndexed { i, warning ->
                    playerManager.getPlayers()
                        .forEach { it.sendMessage(text.formatWarning(warning, i == 0)) }
                }
            }
        }

        if (state.state == GameState.POSTGAME) {
            val hasAnyoneVoted = playerManager.getPlayers().any { readyState.isReady(it.uuid) }
            if (
                !readyState.isRunning &&
                (hasAnyoneVoted || !config.nextRoundWhenReadyWaitsForFirstVote)
            ) {
                config.nextRoundWhenReadySeconds?.let {
                    log.info("[ReadyController] Starting the POSTGAME timer for $it seconds")
                    readyState.startTimer(it.seconds)
                }
            }

            if (isComplete) {
                log.info("[ReadyController] Reached the end of the POSTGAME timer - running reset.")
                serverTaskExecutor.execute {
                    resetService.resetGame()
                }
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
        )

        for (player in playersToUpdate) {
            val isPlayerReady = readyState.isReady(player.uuid)
            val title = when {
                isPlayerReady -> text.string(StringKey.LobbyStartingPlayersReady, updatePacket.readyPlayers, updatePacket.totalPlayers)
                else -> text.string(
                    StringKey.LobbyStartingReadyUp,
                    Text.keybind("key.sneak"), Text.keybind(KEYBIND_OPEN_CARD)
                )
            }
            val subtitle = when {
                state.state == GameState.PREGAME -> text.string(StringKey.LobbyStartingTimeRemaining, updatePacket.remainingDuration.formatHHMMSS())
                else -> text.string(StringKey.LobbyNextRoundTimeRemaining, updatePacket.remainingDuration.formatHHMMSS())
            }

            val packet = updatePacket.copy(
                isReady = isPlayerReady,
                title = title,
                subtitle = subtitle,
                canSendReady = isVotingEnabled && permissions.hasPermission(player, Permission.COMMAND_READY)
            )

            when {
                packets.readyUpdateV3.send(player, packet) -> {}
                packets.readyUpdateV2.send(player, packet) -> {}
                packets.readyUpdateV1.send(player, packet) -> {}
                packet.isRunning -> {
                    val message = when {
                        !isPlayerReady -> text.string(StringKey.LobbyStartingInRunCommand, packet.remainingDuration.formatHHMMSS())
                        else -> text.string(StringKey.LobbyStartingInReady, packet.remainingDuration.formatHHMMSS(), packet.readyPlayers, packet.totalPlayers)
                    }
                    player.sendHotbarMessage(message)
                }
            }
        }
    }

    init {
        events.onStateChange {
            reset()

            // Run an update check immediately
            update()

            // If the timer is not running, send an extra packet to dismiss the ready GUI
            if (!readyState.isRunning) {
                val packet = ReadyUpdatePacket(
                    isRunning = false,
                    isReady = false,
                    state = state.state,
                    remainingDuration = Duration.ZERO,
                    totalDuration = Duration.ZERO,
                    readyPlayers = 0,
                    totalPlayers = 0,
                    title = null,
                    subtitle = null,
                )
                for (player in playerManager.getPlayers()) {
                    when {
                        packets.readyUpdateV3.send(player, packet) -> {}
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
            reset()
        }

        events.onChangeTeam { player ->
            if (readyState.isReady(player.uuid))
                playerManager.updatePlayerListName(player)
        }

        events.onPacket(packets.readySetV1) {
            readyService.setReady(it.player, it.packet.isReady)
        }

        events.onPlayerChannelRegister {
            update(listOf(it.player))
        }

        events.onUpdateTick {
            update()
        }
    }
}