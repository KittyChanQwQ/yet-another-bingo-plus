package me.jfenn.bingo.common.card

import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.card.tierlist.TierLabel

@Serializable
data class BingoCardEntry(
    val objectiveId: String,
    val tier: TierLabel?,
)
