package me.jfenn.bingo.config

import kotlinx.serialization.Serializable

@Serializable
data class BingoConfig(
    var overwrite: Boolean = true,
    var gameMode: BingoMode = BingoMode.STANDARD,
    var winCondition: WinCondition = WinCondition.THREE_LINES,
    var timeLimit: TimeLimit = TimeLimit.ONE_HOUR,
    var itemDistribution: List<Int> = listOf(2, 6, 9, 6, 2),
    var isElytra: Boolean = false,
    var isUnlockRecipes: Boolean = true,
    var isPvpEnabled: Boolean = true,
    var isKeepInventory: Boolean = false,
    var spreadPlayersDistance: Int = 100,
    var unsafeSkipWorldClose: Boolean = false,
) {

    fun isValid(): Boolean {
        return itemDistribution.sum() == 25
    }

}