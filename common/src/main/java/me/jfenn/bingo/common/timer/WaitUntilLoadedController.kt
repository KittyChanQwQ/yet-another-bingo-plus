package me.jfenn.bingo.common.timer

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.mixinhandler.ServerPlayNetworkHandlerMixinHandler
import net.minecraft.server.MinecraftServer
import org.slf4j.Logger
import kotlin.time.Duration.Companion.milliseconds

/**
 * Wait until the player has finished loading terrain
 */
class WaitUntilLoadedController(
    events: ScopedEvents,
    private val options: BingoOptions,
    private val state: BingoState,
    private val countdownService: CountdownService,
    private val teamService: TeamService,
    private val server: MinecraftServer,
    private val log: Logger,
) {

    private var startedLoading: Instant? = null

    init {
        events.onEnter(GameState.LOADING) {
            startedLoading = Clock.System.now()
            log.info("Waiting until all players have loaded spawn terrain...")
        }

        events.onGameTick {
            if (state.state != GameState.LOADING) return@onGameTick

            // +2 ticks to ensure that previous movement events are excluded
            val startedLoading = (startedLoading ?: return@onGameTick) + 40.milliseconds

            val isEveryPlayerLoaded = server.playerManager.playerList
                .filter { teamService.isPlaying(it) }
                .all { player ->
                    val lastMovementPacket = ServerPlayNetworkHandlerMixinHandler.getLastPlayerMovement(player)
                    lastMovementPacket != null && lastMovementPacket > startedLoading
                }

            if (!isEveryPlayerLoaded) return@onGameTick
            log.info("Done! (${(Clock.System.now() - startedLoading)})")

            // change to the playing state
            if (options.startCountdownSeconds > 0) {
                state.changeState(events, GameState.COUNTDOWN)
            } else {
                state.changeState(events, GameState.PLAYING)

                val packet = CountdownPacket(0)
                for (player in server.playerManager.playerList) {
                    countdownService.sendCountdownPacket(player, packet)
                }
            }
        }
    }
}