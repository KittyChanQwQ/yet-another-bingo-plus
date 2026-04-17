package me.jfenn.bingo.common.card

import me.jfenn.bingo.api.card.ICardEntry
import me.jfenn.bingo.api.card.ICardEntryAccess
import me.jfenn.bingo.api.card.ICardObjective
import me.jfenn.bingo.common.card.objective.BingoObjective

internal data class BingoCardEntryAccess(
    val entry: BingoCardEntry,
    val objective: BingoObjective,
) : ICardEntryAccess, ICardEntry by entry, ICardObjective by objective
