package me.jfenn.bingo.config

import net.minecraft.util.Formatting

enum class TierList(
    val formatting: Formatting,
) {
    S(Formatting.DARK_RED),
    A(Formatting.RED),
    B(Formatting.GOLD),
    C(Formatting.YELLOW),
    D(Formatting.GREEN),
}