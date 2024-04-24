package me.jfenn.bingo.common.config

import net.minecraft.util.Formatting

enum class TierLabel(
    val formatting: Formatting,
) {
    S(Formatting.DARK_RED),
    A(Formatting.RED),
    B(Formatting.GOLD),
    C(Formatting.YELLOW),
    D(Formatting.GREEN),
}