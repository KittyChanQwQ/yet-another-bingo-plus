package me.jfenn.bingo.api.options

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class BingoGoal {
    @Serializable
    @SerialName("items")
    class Items(val items: Int): BingoGoal()
    @Serializable
    @SerialName("lines")
    class Lines(val lines: Int): BingoGoal()

    companion object {
        const val MAX_ITEMS = 25
        const val MAX_LINES = 11
    }

    fun isFullCard(): Boolean {
        return when (this) {
            is Items -> items >= MAX_ITEMS
            is Lines -> lines >= MAX_LINES
        }
    }
}