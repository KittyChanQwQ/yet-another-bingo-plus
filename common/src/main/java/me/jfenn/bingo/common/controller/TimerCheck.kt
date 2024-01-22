package me.jfenn.bingo.common.controller

import kotlinx.datetime.Clock
import me.jfenn.bingo.common.GameService
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState

/**
 * Checks when the game time exceeds its time limit, and
 * ends the game when it does.
 */
class TimerCheck(
    events: ScopedEvents,
    private val state: BingoState,
    private val options: BingoOptions,
    private val gameService: GameService,
) {

    init {
        events.onEnter(GameState.PLAYING) { prevState ->
            if (prevState != GameState.PREGAME) return@onEnter

            state.startedAt = Clock.System.now()
        }

        events.onUpdateTick {
            if (state.state != GameState.PLAYING) return@onUpdateTick
            val duration = state.ingameDuration() ?: return@onUpdateTick
            val timeLimit = options.timeLimit ?: return@onUpdateTick

            // set the last updated timestamp
            state.updatedAt = Clock.System.now()

            if (duration >= timeLimit) {
                // end the game!
                gameService.end("Out of time!")
            }
        }
    }

}