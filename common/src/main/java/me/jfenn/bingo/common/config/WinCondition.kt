package me.jfenn.bingo.common.config

import me.jfenn.bingo.generated.StringKey

enum class WinCondition(
    val numLines: Int,
    val string: StringKey,
) {
    ONE_LINE(1, StringKey.GoalOneLine),
    TWO_LINES(2, StringKey.GoalTwoLines),
    THREE_LINES(3, StringKey.GoalThreeLines),
    FOUR_LINES(4, StringKey.GoalFourLines),
    FULL_CARD(12, StringKey.GoalFullCard);
}