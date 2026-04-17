package me.jfenn.bingo.api

import me.jfenn.bingo.api.data.BingoGameStatus
import me.jfenn.bingo.api.data.IBingoGame
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import java.time.Duration
import java.util.*

internal class BingoGameImpl(
    private val state: BingoState,
) : IBingoGame {
    override val id: UUID
        get() = state.gameId

    override val status: BingoGameStatus
        get() = when (state.state) {
            GameState.UNINITIALIZED,
            GameState.PREGAME -> BingoGameStatus.PREGAME
            GameState.STARTING,
            GameState.PRELOADING,
            GameState.LOADING,
            GameState.COUNTDOWN -> BingoGameStatus.STARTING
            GameState.PLAYING -> BingoGameStatus.PLAYING
            GameState.POSTGAME -> BingoGameStatus.POSTGAME
        }

    override val time: Duration?
        get() = state.ingameDuration()

    override val timeRemaining: Duration?
        get() = state.remainingDuration()
}