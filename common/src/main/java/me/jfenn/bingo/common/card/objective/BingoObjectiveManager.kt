package me.jfenn.bingo.common.card.objective

import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.CardGeneratorState
import org.slf4j.Logger

class BingoObjectiveManager(
    private val providers: List<IObjectiveManager>,
    private val log: Logger,
) : IObjectiveManager {
    override fun list(): Iterable<String> {
        return providers.flatMap { it.list() }
    }

    override fun find(id: String, state: CardGeneratorState): BingoObjective? {
        // remove an "advancement!" or "item!" prefix from the id
        val type = id.substringBefore('!')
        val identifier = id.substringAfter('!')

        if (type == "advancement") {
            return providers.find { it is AdvancementObjectiveManager }
                ?.find(identifier, state)
        }

        if (type == "item") {
            return providers.find { it is ItemObjectiveManager }
                ?.find(identifier, state)
        }

        for (provider in providers) {
            val goal = provider.find(identifier, state) ?: continue
            return goal
        }

        log.debug("[BingoGoalProvider] '$identifier' could not be resolved by any objective provider.")
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