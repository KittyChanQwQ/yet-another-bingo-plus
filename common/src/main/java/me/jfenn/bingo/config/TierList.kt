package me.jfenn.bingo.config

import me.jfenn.bingo.utils.text.TextColor

enum class TierList(
    val formatting: TextColor,
) {
    S(TextColor.DARK_RED),
    A(TextColor.RED),
    B(TextColor.GOLD),
    C(TextColor.YELLOW),
    D(TextColor.GREEN),
}