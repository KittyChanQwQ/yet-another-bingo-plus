package me.jfenn.bingo.common.card.tierlist

import me.jfenn.bingo.api.card.ObjectiveTier
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatTitle
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.text.Formatting

enum class TierLabel(
    val tier: ObjectiveTier,
    val formatting: Formatting,
    val string: StringKey,
    val shortString: StringKey,
    val tooltipString: StringKey,
) {
    S(ObjectiveTier.S, Formatting.DARK_RED, StringKey.TierS, StringKey.TierSShort, StringKey.TierSTooltip),
    A(ObjectiveTier.A, Formatting.RED, StringKey.TierA, StringKey.TierAShort, StringKey.TierATooltip),
    B(ObjectiveTier.B, Formatting.GOLD, StringKey.TierB, StringKey.TierBShort, StringKey.TierBTooltip),
    C(ObjectiveTier.C, Formatting.YELLOW, StringKey.TierC, StringKey.TierCShort, StringKey.TierCTooltip),
    D(ObjectiveTier.D, Formatting.GREEN, StringKey.TierD, StringKey.TierDShort, StringKey.TierDTooltip);

    fun text(text: TextProvider) = text.string(this.string).formatted(formatting)

    companion object {
        const val LIST_EASY = "easy"
        val DIFFICULTY_EASY = listOf(0, 0, 0, 9, 16)
        val DIFFICULTY_PRESETS = mapOf(
            LIST_EASY to DIFFICULTY_EASY,
            "medium" to listOf(0, 0, 3, 10, 12),
            "hard" to listOf(0, 1, 5, 10, 9),
            "extreme" to listOf(0, 5, 7, 7, 6),
            "impossible" to listOf(5, 7, 6, 5, 2),
        )

        fun presetText(text: TextProvider, difficulty: String) = text.translatable("bingo.card_difficulty.$difficulty", difficulty.formatTitle())
    }
}