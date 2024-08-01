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

class AllOfObjectiveManager(
    private val state: BingoState,
    private val options: BingoOptions,
    private val objectiveRegistryState: ObjectiveRegistryState,
    private val objectiveDisplayService: ObjectiveDisplayService,
    private val objectiveService: ObjectiveService,
    private val tagExpansionService: TagExpansionService,
    private val text: TextProvider,
) : IObjectiveManager {
    override fun find(id: String, state: CardGeneratorState): BingoObjective? {
        val data = objectiveRegistryState.objectives[id] as? ObjectiveData.AllOf
            ?: return null

        return BingoObjective.AllOfEntry(
            id = id,
            allOfObjectives = tagExpansionService.expandItemTags(data.objectives),
            displayData = data.display,
        ).also {
            it.conflictsWithObjectives.addAll(data.conflictsWith)
        }
    }

    override fun init(card: BingoCard) {
        for (objective in card.objectivesByInstance<BingoObjective.AllOfEntry>()) {
            val objectives = objective.allOfObjectives.mapNotNull { card.objectives[it] }
            val displayNames = objectives.mapNotNull { it.display.name }
            val display = ObjectiveDisplay.Resolved(
                name = text.string(StringKey.ObjectiveAllOf, Text.empty().append(displayNames.firstOrNull() ?: Text.empty()).append(", ...")),
                lore = displayNames,
                item = objectives.firstNotNullOfOrNull { it.display.item },
            )

            objective.display = objectiveDisplayService.resolve(objective.id, objective.displayData, display)
        }
    }

    override fun tick(card: BingoCard) {
        for (objective in card.objectivesByInstance<BingoObjective.AllOfEntry>()) {
            val objectives = objective.allOfObjectives.mapNotNull { card.objectives[it] }

            objective.teamsOnceAchieved.addAll(objective.teams)

            objective.teamsProgress = objectives.flatMap { it.teamsProgress.entries }
                .groupBy { it.key }
                .mapValues { (_, entries) ->
                    entries.map { it.value }.sum() /
                            objectives.size.coerceAtLeast(1).toFloat()
                }
                .toMutableMap()

            objective.teamsSeen = objectiveService.getTeamsSeen(objective) { team ->
                objectives.any { it.hasSeen(team) }
            }

            objective.playersHolding = objectives
                .flatMap { it.playersHolding.entries }
                .groupBy { it.key }
                .filter { it.value.size == objectives.size } // in order to capture, must be holding every objective
                .mapValues { (_, captures) -> captures.map { it.value }.maxBy { it.instant } }
                .toMutableMap()

            objective.players = objectiveService.getPlayers(objective)

            objective.teams = objectiveService.getTeams(objective)
        }
    }
}