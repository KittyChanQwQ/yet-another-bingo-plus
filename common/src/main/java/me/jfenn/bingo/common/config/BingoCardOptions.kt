package me.jfenn.bingo.common.config

import kotlinx.serialization.Serializable

@Serializable
data class BingoCardOptions(
    var goal: BingoGoal = BingoGoal.Lines(3),
    var itemDistribution: List<Int> = listOf(0, 5, 7, 7, 6),
    var tierLists: Map<String, Int> = mapOf("all_items" to 1, "challenge" to 1),

    // game mode settings
    var isLockoutMode: Boolean = false,
    var isInventoryMode: Boolean = false,
    var isHiddenItemsMode: Boolean = false,
) {
    fun isValid(): Boolean {
        return itemDistribution.sum() <= 25 && tierLists.isNotEmpty()
    }
}
