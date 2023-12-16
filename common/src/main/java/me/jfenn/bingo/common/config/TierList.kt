package me.jfenn.bingo.common.config

import me.jfenn.bingo.common.utils.text.TextColor

enum class TierList(
    val formatting: TextColor,
) {
    S(TextColor.DARK_RED),
    A(TextColor.RED),
    B(TextColor.GOLD),
    C(TextColor.YELLOW),
    D(TextColor.GREEN),
}