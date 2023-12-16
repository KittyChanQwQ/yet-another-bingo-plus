package me.jfenn.bingo.common.integrations

import kotlinx.datetime.Clock
import me.jfenn.bingo.common.Bingo
import me.jfenn.bingo.common.BingoGame
import me.jfenn.bingo.common.state.GameState

/**
 * Checks when the game time exceeds its time limit, and
 * ends the game when it does.
 */
object TimerCheck {

    fun init() {
        GameState.PLAYING.onEnter { prevState ->
            if (prevState != GameState.PREGAME) return@onEnter

            state.startedAt = Clock.System.now()
        }

        Bingo.onUpdateTick {
            if (state.state != GameState.PLAYING) return@onUpdateTick
            val duration = state.ingameDuration() ?: return@onUpdateTick
            val timeLimit = config.timeLimit ?: return@onUpdateTick

            // set the last updated timestamp
            state.updatedAt = Clock.System.now()

            if (duration >= timeLimit) {
                // end the game!
                BingoGame.end(this, "Out of time!")
            }
        }
    }

}