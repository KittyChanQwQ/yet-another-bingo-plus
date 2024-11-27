package me.jfenn.bingo.api

import me.jfenn.bingo.api.annotations.BingoInternalApi
import java.util.*

object BingoApi {
    @JvmStatic
    private var current: IBingoApi? = null

    @BingoInternalApi
    @JvmStatic
    fun set(api: IBingoApi?) { current = api }

    @JvmStatic
    private fun getCurrent(): IBingoApi {
        return current ?: error("Yet Another Bingo is not initialized!")
    }

    @JvmStatic
    val game get() = getCurrent().game

    @JvmStatic
    val teams get() = getCurrent().teams

    @JvmStatic
    fun getPlayerStats(id: UUID) = getCurrent().getPlayerStats(id)
}