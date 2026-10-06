package me.jfenn.bingo.api

import me.jfenn.bingo.api.annotations.BingoInternalApi
import me.jfenn.bingo.api.internal.ExtensionManager.inventoryProviders
import me.jfenn.bingo.api.provider.IInventoryProvider

/**
 * The primary entrypoint for interacting with the Yet Another Bingo (server-side) API.
 */
object BingoApi {
    /**
     * Used to obtain an instance of the API.
     *
     * Note that this will be null at different points in the server lifecycle; it is populated in SERVER_STARTED.
     *
     * It is safe to hold a reference to this on the server, but it will be cleared and recreated between singleplayer levels.
     *
     * Use [BingoEvents.INIT] and [BingoEvents.CLOSE] to safely obtain (and clear) a reference to this object.
     */
    @JvmStatic
    var INSTANCE: IBingoApi? = null
        private set

    /**
     * @suppress
     */
    @BingoInternalApi
    @JvmStatic
    fun set(api: IBingoApi?) { INSTANCE = api }

    /**
     * Register an inventory provider. This can supply additional inventories for a player, such as backpack mods or trinkets.
     */
    @OptIn(BingoInternalApi::class)
    @JvmStatic
    fun registerInventoryProvider(provider: IInventoryProvider) {
        inventoryProviders.add(provider)
    }

    /**
     * Unregister an inventory provider.
     */
    @OptIn(BingoInternalApi::class)
    @JvmStatic
    fun unregisterInventoryProvider(provider: IInventoryProvider) {
        inventoryProviders.remove(provider)
    }
}