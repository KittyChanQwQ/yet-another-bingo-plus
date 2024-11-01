package me.jfenn.bingo.common.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class BingoWinCondition {
    @Serializable
    @SerialName("cards")
    data class Cards(val cards: Int): BingoWinCondition()
    @Serializable
    @SerialName("infinite")
    data object Infinite: BingoWinCondition()
}
