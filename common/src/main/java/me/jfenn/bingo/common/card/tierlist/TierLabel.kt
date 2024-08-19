package me.jfenn.bingo.common.card.tierlist

import net.minecraft.text.Text
import net.minecraft.util.Formatting

enum class TierLabel(
    val formatting: Formatting,
) {
    S(Formatting.DARK_RED),
    A(Formatting.RED),
    B(Formatting.GOLD),
    C(Formatting.YELLOW),
    D(Formatting.GREEN);

    fun text(): Text = Text.literal("$this-tier").formatted(formatting)
}