package me.jfenn.bingo.common.timer

import kotlinx.datetime.Clock
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.game.GameEndReason
import me.jfenn.bingo.common.game.GameService
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import org.slf4j.Logger
import kotlin.time.Duration.Companion.seconds

/**
 * Checks when the game time exceeds its time limit, and
 * ends the game when it does.
 */
class TimerCheck(
    events: ScopedEvents,
    private val state: BingoState,
    private val options: BingoOptions,
    private val gameService: GameService,
    private val log: Logger,
) : BingoComponent() {

    init {
        events.onEnter(GameState.PLAYING) { prevState ->
            if (prevState == GameState.PLAYING) return@onEnter

            state.startedAt = Clock.System.now()
        }

        events.onGameTick {
            if (state.state != GameState.PLAYING) return@onGameTick

            val now = Clock.System.now()

            // if significant time has elapsed since the last update, the game might have been paused/offline...
            state.updatedAt
                ?.let { lastUpdate -> now - lastUpdate }
                // assumes the server achieves at least 1tps, or onGameTick is called more than once per second
                ?.takeIf { it > 1.seconds }
                ?.let { pauseDuration ->
                    log.info("Game was inactive for $pauseDuration - updating timer")
                    state.timeOffline += pauseDuration
                }

            // set the last updated timestamp
            state.updatedAt = now

            val duration = state.ingameDuration() ?: return@onGameTick
            val timeLimit = options.timeLimit
            if (timeLimit != null && duration >= timeLimit) {
                // end the game!
                gameService.end(GameEndReason.TIMER)
            }
        }
    }

}