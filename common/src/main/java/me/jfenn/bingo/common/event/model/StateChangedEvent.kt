package me.jfenn.bingo.common.event.model

import me.jfenn.bingo.api.state.GameState
import me.jfenn.bingo.platform.event.IEvent

data class StateChangedEvent(
    val from: GameState,
    val to: GameState,
) {
    companion object : IEvent<StateChangedEvent>
}
