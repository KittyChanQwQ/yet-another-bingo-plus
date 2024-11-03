package me.jfenn.bingo.common.card.objective

import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.CardGeneratorState
import me.jfenn.bingo.common.card.TagExpansionService
import me.jfenn.bingo.common.card.data.ObjectiveData
import me.jfenn.bingo.common.card.data.ObjectiveRegistryState
import me.jfenn.bingo.common.map.CardTile
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text
import org.slf4j.Logger

class OneOfObjectiveManager(
    private val log: Logger,
    private val objectiveRegistryState: ObjectiveRegistryState,
    private val objectiveDisplayService: ObjectiveDisplayService,
    private val objectiveService: ObjectiveService,
    private val tagExpansionService: TagExpansionService,
    private val text: TextProvider,
) : IObjectiveManager {
    override fun list(): Iterable<String> {
        return objectiveRegistryState.rootObjectives
            .filter { (_, it) -> it is ObjectiveData.OneOf }
            .map { it.key }
            .toList()
    }

    override fun find(id: String, state: CardGeneratorState): BingoObjective? {
        val data = objectiveRegistryState.objectives[id] as? ObjectiveData.OneOf
            ?: return null

        if (data.objectives.isEmpty()) {
            log.error("[OneOfObjectiveManager] $id 'objectives' cannot be empty!")
            return null
        }

        return BingoObjective.OneOfEntry(
            id = id,
            data = data,
            oneOfObjectives = data.objectives
                .map { data.expandReference(id, it) }
                .let { tagExpansionService.expandItemTags(it) },
        )
    }

    override fun init(card: BingoCard) {
        for (objective in card.objectivesByInstance<BingoObjective.OneOfEntry>()) {
            val objectives = objective.oneOfObjectives.mapNotNull { card.objectives[it] }
            val displayNames = objectives.mapNotNull { it.display.name }
            val display = ObjectiveDisplay.Resolved(
                name = text.string(StringKey.ObjectiveOneOf, Text.empty().append(displayNames.firstOrNull() ?: Text.empty()).append(", ...")),
                lore = displayNames,
                item = objectives.firstNotNullOfOrNull { it.display.item },
                decoration = CardTile.Decoration.MULTI_ITEM,
            )

            objective.display = objectiveDisplayService.resolve(objective.id, objective.data.display, display)
        }
    }

    override fun tick(card: BingoCard) {
        for (objective in card.objectivesByInstance<BingoObjective.OneOfEntry>()) {
            val objectives = objective.oneOfObjectives.mapNotNull { card.objectives[it] }

            objectiveService.updateTeamsOnceAchieved(objective)

            objectiveService.updateTeamsSeen(card, objective) { team ->
                objectives.any { it.hasSeen(team) }
            }

            val teamsProgress = objectives.flatMap { it.teamsProgress.entries }
                .groupBy { it.key }
                .mapValues { (_, entries) -> entries.maxOf { it.value } }

            val playersHolding = objectives
                .flatMap { it.playersHolding.entries }
                .groupBy { it.key }
                .mapValues { (_, captures) -> captures.map { it.value }.maxBy { it.instant } }
                .let {
                    when {
                        objective.data.permanent -> it + objective.playersHolding
                        else -> it
                    }
                }

            val players = objectiveService.getPlayers(
                card = card,
                playersHolding = playersHolding,
                players = objective.players,
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