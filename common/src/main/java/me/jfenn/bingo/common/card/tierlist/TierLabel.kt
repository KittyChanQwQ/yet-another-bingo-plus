package me.jfenn.bingo.common.card.tierlist

import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text
import net.minecraft.util.Formatting

enum class TierLabel(
    val formatting: Formatting,
    val string: StringKey,
) {
    S(Formatting.DARK_RED, StringKey.TierS),
    A(Formatting.RED, StringKey.TierA),
    B(Formatting.GOLD, StringKey.TierB),
    C(Formatting.YELLOW, StringKey.TierC),
    D(Formatting.GREEN, StringKey.TierD);

    fun text(): Text = Text.literal("$this-tier").formatted(formatting)
}