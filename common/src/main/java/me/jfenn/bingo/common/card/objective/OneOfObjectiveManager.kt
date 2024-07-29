package me.jfenn.bingo.common.card.objective

import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.CardGeneratorState
import me.jfenn.bingo.common.card.TagExpansionService
import me.jfenn.bingo.common.card.data.ObjectiveData
import me.jfenn.bingo.common.card.data.ObjectiveRegistryState
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class OneOfObjectiveManager(
    private val state: BingoState,
    private val options: BingoOptions,
    private val objectiveRegistryState: ObjectiveRegistryState,
    private val objectiveDisplayService: ObjectiveDisplayService,
    private val tagExpansionService: TagExpansionService,
    private val text: TextProvider,
) : IObjectiveManager {
    override fun find(id: String, state: CardGeneratorState): BingoObjective? {
        val data = objectiveRegistryState.objectives[id] as? ObjectiveData.OneOf
            ?: return null

        return BingoObjective.OneOfEntry(
            id = id,
            oneOfObjectives = tagExpansionService.expandItemTags(data.objectives),
            displayData = data.display,
        ).also {
            it.conflictsWithObjectives.addAll(data.conflictsWith)
        }
    }

    override fun init(card: BingoCard) {
        for (objective in card.objectivesByInstance<BingoObjective.OneOfEntry>()) {
            val objectives = objective.oneOfObjectives.mapNotNull { card.objectives[it] }
            val displayNames = objectives.mapNotNull { it.display.name }
            val display = ObjectiveDisplay.Resolved(
                name = text.string(StringKey.ObjectiveOneOf, Text.empty().append(displayNames.firstOrNull() ?: Text.empty()).append(", ...")),
                lore = displayNames,
                item = objectives.firstNotNullOfOrNull { it.display.item },
            )

            objective.display = objectiveDisplayService.resolve(objective.id, objective.displayData, display)
        }
    }

    override fun tick(card: BingoCard) {
        for (objective in card.objectivesByInstance<BingoObjective.OneOfEntry>()) {
            val objectives = objective.oneOfObjectives.mapNotNull { card.objectives[it] }

            for (team in state.getRegisteredTeams()) {
                val hasSeen = objectives.any { it.hasSeen(team.key) }
                val hasAchieved = objectives.any { it.hasAchieved(team.key) }

                val progress = objectives.maxOf { it.getProgress(team.key) }
                objective.setProgress(team.key, progress)

                // If lockout mode, prevent new teams from achieving the objective
                if (hasAchieved && options.isLockoutMode && objective.hasAnyAchieved())
                    continue

                if (hasSeen) objective.markSeen(team.key)
                val hasChanged = objective.setAchieved(team.key, hasAchieved)

                if (hasAchieved) {
                    if (hasChanged) {
                        objectives
                            .flatMap { it.players.entries }
                            .groupBy { it.key }
                            .mapNotNull { (key, value) ->
                                value.maxOfOrNull { it.value }?.let { key to it }
                            }
                            .forEach { (key, value) ->
                                objective.addPlayer(key, value)
                            }
                    }
                } else {
                    objective.removePlayers(team.players)
                }
            }
        }
    }
}