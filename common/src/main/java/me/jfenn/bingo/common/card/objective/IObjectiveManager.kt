package me.jfenn.bingo.common.card.objective

import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.CardGeneratorState

interface IObjectiveManager {
    fun find(id: String, state: CardGeneratorState): BingoObjective?
    fun init(card: BingoCard)
    fun tick(card: BingoCard)
}
