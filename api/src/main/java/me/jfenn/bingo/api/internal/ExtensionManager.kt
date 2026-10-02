package me.jfenn.bingo.api.internal

import me.jfenn.bingo.api.annotations.BingoInternalApi
import me.jfenn.bingo.api.event.EventListener
import me.jfenn.bingo.api.provider.IInventoryProvider
import me.jfenn.bingo.api.state.GameState
import me.jfenn.bingo.api.state.GameStateChangedEvent

@BingoInternalApi
object ExtensionManager {
    val inventoryProviders = mutableListOf<IInventoryProvider>()
    val gameStateListeners = mutableMapOf<GameState, EventListener<GameStateChangedEvent>>()
}