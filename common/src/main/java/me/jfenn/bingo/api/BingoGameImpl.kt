package me.jfenn.bingo.api

import me.jfenn.bingo.api.data.BingoGameStatus
import me.jfenn.bingo.api.data.IBingoGame
import me.jfenn.bingo.api.state.GameAction
import me.jfenn.bingo.api.state.GameState
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.platform.event.IEventBus
import java.time.Duration
import java.util.*

internal class BingoGameImpl(
    private val bingoState: BingoState,
    private val events: IEventBus,
) : IBingoGame {
    override val id: UUID
        get() = bingoState.gameId

    override val status: BingoGameStatus
        get() = bingoState.state.status

    override var state: GameState
        get() = bingoState.state
        set(value) {
            bingoState.changeState(events, value)
        }

    override fun changeState(action: GameAction) {
        bingoState.changeState(events, action)
    }

    override val time: Duration?
        get() = bingoState.ingameDuration()

    override val timeRemaining: Duration?
        get() = bingoState.remainingDuration()
}