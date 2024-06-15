package me.jfenn.bingo.common.autorestart

import kotlinx.datetime.Clock
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatStringSmall
import me.jfenn.bingo.generated.StringKey
import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.slf4j.Logger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class RestartTimerController(
    events: ScopedEvents,
    private val log: Logger,
    private val server: MinecraftServer,
    private val timerState: RestartTimerState,
    private val config: BingoConfig,
    private val text: TextProvider,
    state: BingoState,
) : BingoComponent() {

    private val sentWarnings = mutableSetOf<Int>()

    private fun broadcastWarning(seconds: Int) {
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

    init {
        if (FabricLoader.getInstance().environmentType == EnvType.SERVER && state.isLobbyMode) {
            events.onEnter(GameState.POSTGAME) {
                val shutdownSeconds = config.shutDownWhenGameOverSeconds ?: return@onEnter

                // Send a warning when the restart is scheduled
                if (shutdownSeconds > 0) {
                    broadcastWarning(shutdownSeconds)
                }

                val shutdownAt = Clock.System.now() + shutdownSeconds.seconds
                log.info("[RestartTimerController] Scheduled a shutdown for {} seconds in the future - {}", shutdownSeconds, shutdownAt)
                timerState.shutdownAt = shutdownAt
            }

            events.onUpdateTick {
                val shutdownAt = timerState.shutdownAt ?: return@onUpdateTick

                val durationLeft = shutdownAt - Clock.System.now()

                if (durationLeft <= Duration.ZERO) {
                    log.info("[RestartTimerController] Reached the end of the bingo restartTimer - shutting down.")
                    server.stop(false)
                    return@onUpdateTick
                }

                val secondsLeft = durationLeft.inWholeSeconds
                if (secondsLeft in 1..5) {
                    broadcastWarning(secondsLeft.toInt())
                }
            }
        }
    }

}