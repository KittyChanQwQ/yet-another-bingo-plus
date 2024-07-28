package me.jfenn.bingo.common.card.objective

import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.CardGeneratorState
import org.slf4j.Logger

class BingoObjectiveManager(
    private val providers: List<IObjectiveManager>,
    private val log: Logger,
) : IObjectiveManager {

    override fun find(id: String, state: CardGeneratorState): BingoObjective? {
        for (provider in providers) {
            val goal = provider.find(id, state) ?: continue
            return goal
        }

        log.error("[BingoGoalProvider] Could not resolve objective id '$id'")
        return null
    }

    override fun init(card: BingoCard) {
        for (provider in providers)
            provider.init(card)
    }

    override fun tick(card: BingoCard) {
        for (provider in providers) {
            provider.tick(card)
        }
    }
}