package me.jfenn.bingo.api.internal

import me.jfenn.bingo.api.annotations.BingoInternalApi
import me.jfenn.bingo.api.provider.IInventoryProvider

@BingoInternalApi
object ExtensionManager {
    val inventoryProviders = mutableListOf<IInventoryProvider>()
}