package me.jfenn.bingo.config

import kotlinx.serialization.Serializable
import me.jfenn.bingo.card.TimeLimit
import me.jfenn.bingo.card.WinCondition

@Serializable
data class BingoConfig(
    var overwrite: Boolean = true,
    var winCondition: WinCondition = WinCondition.THREE_LINES,
    var timeLimit: TimeLimit = TimeLimit.ONE_HOUR,
    var itemDistribution: List<Int> = listOf(2, 6, 9, 6, 2),
) {

    fun isValid(): Boolean {
        return itemDistribution.sum() == 25
    }

}