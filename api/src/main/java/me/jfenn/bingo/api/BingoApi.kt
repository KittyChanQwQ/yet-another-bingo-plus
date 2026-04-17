package me.jfenn.bingo.api

import me.jfenn.bingo.api.annotations.BingoInternalApi
import me.jfenn.bingo.api.internal.ExtensionManager.inventoryProviders
import me.jfenn.bingo.api.provider.IInventoryProvider

object BingoApi {
    @JvmStatic
    var INSTANCE: IBingoApi? = null
        private set

    @BingoInternalApi
    @JvmStatic
    fun set(api: IBingoApi?) { INSTANCE = api }

    @OptIn(BingoInternalApi::class)
    @JvmStatic
    fun registerInventoryProvider(provider: IInventoryProvider) {
        inventoryProviders.add(provider)
    }

    @OptIn(BingoInternalApi::class)
    @JvmStatic
    fun unregisterInventoryProvider(provider: IInventoryProvider) {
        inventoryProviders.remove(provider)
    }
}