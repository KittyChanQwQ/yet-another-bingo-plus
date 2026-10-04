package me.jfenn.bingo.common.reset

import me.jfenn.bingo.api.state.GameState

data class ResetOptions(
    val newState: GameState = GameState.PREGAME,
    val worldSeed: Long? = null,
    val cardSeed: Long? = null,
)