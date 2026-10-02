package me.jfenn.bingo.api.state

class GameStateChangedEvent(
    @JvmField val from: GameState,
    @JvmField val to: GameState,
)
