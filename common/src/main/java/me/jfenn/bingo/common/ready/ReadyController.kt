package me.jfenn.bingo.common.ready

import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.event.packet.ServerPacketEvents
import me.jfenn.bingo.common.game.GameService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatHHMMSS
import me.jfenn.bingo.common.utils.formatStringSmall
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.integrations.PermissionsIntegration
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.slf4j.Logger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ReadyController(
    private val log: Logger,
    private val readyState: ReadyTimerState,
    private val state: BingoState,
    private val server: MinecraftServer,
    private val config: BingoConfig,
    private val text: TextProvider,
    private val gameService: GameService,
    private val playerManager: IPlayerManager,
    private val teamService: TeamService,
    private val packets: ServerPacketEvents,
    private val permissions: PermissionsIntegration,
    events: ScopedEvents,
) {

    private val sentWarnings = mutableSetOf<Int>()

    private fun broadcastRestartWarning(seconds: Int) {
        if (seconds !in 1..5) return
        if (seconds in sentWarnings)
            return

        sentWarnings.add(seconds)
        server.playerManager.broadcast(
            Text.literal("⚠ ")
                .append(text.string(StringKey.CommandRestartWarning, seconds.seconds.formatStringSmall()))
                .formatted(Formatting.GOLD),
            false
        )
    }

    private fun isPregameReady(): Boolean {
        val players = playerManager.getPlayers()
        val isReady = players.all { teamService.isPlaying(it) } || players.any { readyState.isReady(it.uuid) }
        return isReady && players.size > 1 && state.options.isValid()
    }

    init {
        events.onStateChange {
            readyState.reset()
        }

        events.onChangeOptions {
            log.info("[ReadyController] Reset the timer, as the game options have changed")
            readyState.reset()
        }

        events.onPacket(packets.readySetV1) {
            if (permissions.hasPermission(it.player, Permission.COMMAND_READY)) {
                readyState.setReady(it.player.uuid, it.packet.isReady)
            }
        }

        events.onUpdateTick {
            if (!state.isLobbyMode) return@onUpdateTick
            if (state.state != GameState.PREGAME && state.state != GameState.POSTGAME)
                return@onUpdateTick

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
                    readyState.reset()
                }

                if (isComplete) {
                    log.info("[ReadyController] Reached the end of the PREGAME timer - starting the game!")
                    gameService.start()
                }
            }

            if (state.state == GameState.POSTGAME) {
                val hasAnyoneVoted = playerManager.getPlayers().any { readyState.isReady(it.uuid) }
                if (
                    !readyState.isRunning &&
                    (hasAnyoneVoted || !config.shutDownWhenReadyWaitsForFirstVote)
                ) {
                    config.shutDownWhenReadySeconds?.let {
                        log.info("[ReadyController] Starting the POSTGAME timer for $it seconds")
                        readyState.startTimer(it.seconds)
                    }
                }

                if (readyState.isRunning) {
                    broadcastRestartWarning(remainingTime?.inWholeSeconds?.toInt() ?: -1)
                }

                if (isComplete) {
                    log.info("[ReadyController] Reached the end of the POSTGAME timer - shutting down.")
                    server.stop(false)
                }
            }

            val updatePacket = ReadyUpdatePacket(
                isRunning = readyState.isRunning,
                isReady = false,
                state = state.state,
                remainingDuration = readyState.remainingTime() ?: Duration.ZERO,
                totalDuration = readyState.totalTime() ?: Duration.ZERO,
                readyPlayers = playerManager.getPlayers().count { readyState.isReady(it.uuid) },
                totalPlayers = playerManager.getPlayers().size,
            )

            for (player in playerManager.getPlayers()) {
                val isPlayerReady = readyState.isReady(player.uuid)
                val packet = updatePacket.copy(isReady = isPlayerReady)

                when {
                    packets.readyUpdateV1.send(player, packet) -> {}
                    packet.isRunning -> {
                        val message = when {
                            !isPlayerReady -> text.string(StringKey.LobbyStartingInRunCommand, packet.remainingDuration.formatHHMMSS())
                            else -> text.string(StringKey.LobbyStartingInReady, packet.remainingDuration.formatHHMMSS(), packet.readyPlayers, packet.totalPlayers)
                        }
                        player.player.sendMessage(message, true)
                    }
                }
            }
        }
    }
}