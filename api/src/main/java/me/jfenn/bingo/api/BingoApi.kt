package me.jfenn.bingo.api

import me.jfenn.bingo.api.internal.ExtensionManager.inventoryProviders
import me.jfenn.bingo.api.annotations.BingoInternalApi
import me.jfenn.bingo.api.provider.IInventoryProvider
import java.util.*

object BingoApi {
    @JvmStatic
    private var current: IBingoApi? = null

    @BingoInternalApi
    @JvmStatic
    fun set(api: IBingoApi?) { current = api }

    @JvmStatic
    val game get() = current?.game

    @JvmStatic
    val teams get() = current?.teams

    @JvmStatic
    fun getPlayerStats(id: UUID) = current?.getPlayerStats(id)

    @JvmStatic
    val config get() = current?.config

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