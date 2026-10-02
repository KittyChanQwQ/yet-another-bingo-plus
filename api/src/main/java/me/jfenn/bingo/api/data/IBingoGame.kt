package me.jfenn.bingo.api.data

import me.jfenn.bingo.api.state.GameAction
import me.jfenn.bingo.api.state.GameState
import java.time.Duration
import java.util.*

interface IBingoGame {
    val id: UUID
    val status: BingoGameStatus

    var state: GameState
    fun changeState(action: GameAction)

    /**
     * The in-game duration for the current round. This is null if the round has not been started.
     */
    val time: Duration?

    /**
     * The time remaining for the current round. This is null if the round has not been started, or if there is no time limit.
     */
    val timeRemaining: Duration?
}

enum class BingoGameStatus {
    PREGAME,
    STARTING,
    PLAYING,
    POSTGAME
}