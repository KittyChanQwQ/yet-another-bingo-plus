package me.jfenn.bingo.common.card.objective

import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.CardGeneratorState
import me.jfenn.bingo.common.card.TagExpansionService
import me.jfenn.bingo.common.card.data.ObjectiveData
import me.jfenn.bingo.common.card.data.ObjectiveRegistryState
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text

class OneOfObjectiveManager(
    private val state: BingoState,
    private val objectiveRegistryState: ObjectiveRegistryState,
    private val objectiveDisplayService: ObjectiveDisplayService,
    private val objectiveService: ObjectiveService,
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

            objectiveService.updateTeamsOnceAchieved(objective)

            objectiveService.updateTeamsSeen(objective) { team ->
                objectives.any { it.hasSeen(team) }
            }

            val teamsProgress = objectives.flatMap { it.teamsProgress.entries }
                .groupBy { it.key }
                .mapValues { (_, entries) -> entries.maxOf { it.value } }

            val playersHolding = objectives
                .flatMap { it.playersHolding.entries }
                .groupBy { it.key }
                .mapValues { (_, captures) -> captures.map { it.value }.maxBy { it.instant } }

            val players = objectiveService.getPlayers(
                playersHolding = playersHolding,
                players = objective.players
            )

            val teams = objectiveService.getTeams(
                players = players
            )

            objectiveService.update(
                objective = objective,
                teamsProgress = teamsProgress,
                playersHolding = playersHolding,
                players = players,
                teams = teams
            )
        }
    }
}