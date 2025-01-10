package me.jfenn.bingo.common.card.objective

import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.CardGeneratorState
import me.jfenn.bingo.common.card.data.ObjectiveRequirements
import me.jfenn.bingo.common.card.filter.ObjectiveFilterList
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.joinText
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.slf4j.Logger

private val NON_ITALIC_STYLE = Style.EMPTY.withItalic(false)

class BingoObjectiveManager(
    private val providers: List<IObjectiveManager>,
    private val text: TextProvider,
    private val log: Logger,
) : IObjectiveManager {
    override fun list(): Iterable<String> {
        return providers.flatMap { it.list() }
    }

    override fun listExcludedIds(): Iterable<String> = sequence {
        for (provider in providers) {
            yieldAll(provider.listExcludedIds())
        }
    }.asIterable()

    private fun checkHardRequirements(
        reqs: ObjectiveRequirements,
    ): Boolean {
        val hasAllObjectives = reqs.objectives.all {
            find(it, CardGeneratorState.DEFAULT) != null
        }

        return hasAllObjectives
    }

    private fun findFromProviders(type: String?, identifier: String, state: CardGeneratorState): BingoObjective? {
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

        return null
    }

    override fun find(id: String, state: CardGeneratorState): BingoObjective? {
        // remove an "advancement!" or "item!" prefix from the id
        val type = id.substringBefore('!')
        val identifier = id.substringAfter('!')

        return findFromProviders(type, identifier, state)
            ?.takeIf { objective ->
                objective.data?.validate
                    ?.let { checkHardRequirements(it) }
                    ?: true
            }
    }

    override fun init(card: BingoCard) {
        // Initialize objectives
        if (!card.isInitialized) {
            // No, this isn't a bug...
            // init() needs to be called multiple times to populate display info on dependent objectives
            initInternal(card)
            initInternal(card)
            initInternal(card)

            card.isInitialized = true
        }
    }

    private fun initInternal(card: BingoCard) {
        for (provider in providers)
            provider.init(card)

        for (entry in card.entries) {
            val objective = card.objectives[entry.objectiveId]

            if (objective == null) {
                log.error("[BingoObjectiveManager] Objective data for '${entry.objectiveId}' is missing!")
                continue
            }

            val lore = buildList {
                objective.display.lore?.let { addAll(it) }

                val desc = listOfNotNull(
                    entry.tier?.let { text.string(it.string).formatted(it.formatting) },
                    entry.source?.let { ObjectiveFilterList.formatName(it).formatted(Formatting.BLUE) },
                    entry.tileName?.let { Text.literal(it).formatted(Formatting.GRAY) },
                )

                desc.takeIf { it.isNotEmpty() }
                    ?.joinText(Text.literal(" • ").formatted(Formatting.GRAY))
                    ?.let { add(it) }
            }.takeIf { it.isNotEmpty() }

            val item = objective.display.item?.copy()?.also { stack ->
                stack.setDisplay(
                    name = objective.display.name
                        ?.takeIf { it != stack.displayName }
                        ?.let { Text.empty().append(it).setStyle(NON_ITALIC_STYLE) },
                    lore = lore
                        ?.map { Text.empty().append(it).setStyle(NON_ITALIC_STYLE) },
                )
                stack.setHideFlags(255)
            }

            entry.display = objective.display.copy(lore = lore, item = item)
        }
    }

    override fun tick(card: BingoCard) {
        for (provider in providers) {
            provider.tick(card)
        }
    }
}