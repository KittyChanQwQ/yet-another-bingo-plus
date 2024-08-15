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

class AllOfObjectiveManager(
    private val objectiveRegistryState: ObjectiveRegistryState,
    private val objectiveDisplayService: ObjectiveDisplayService,
    private val objectiveService: ObjectiveService,
    private val tagExpansionService: TagExpansionService,
    private val text: TextProvider,
) : IObjectiveManager {
    override fun list(): Iterable<String> {
        return objectiveRegistryState.objectives
            .filterValues { it is ObjectiveData.AllOf }
            .keys
    }

    override fun find(id: String, state: CardGeneratorState): BingoObjective? {
        val data = objectiveRegistryState.objectives[id] as? ObjectiveData.AllOf
            ?: return null

        return BingoObjective.AllOfEntry(
            id = id,
            data = data,
            allOfObjectives = tagExpansionService.expandItemTags(data.objectives),
        )
    }

    override fun init(card: BingoCard) {
        for (objective in card.objectivesByInstance<BingoObjective.AllOfEntry>()) {
            val objectives = objective.allOfObjectives.mapNotNull { card.objectives[it] }
            val displayNames = objectives.mapNotNull { it.display.name }
            val display = ObjectiveDisplay.Resolved(
                name = text.string(StringKey.ObjectiveAllOf, Text.empty().append(displayNames.firstOrNull() ?: Text.empty()).append(", ...")),
                lore = displayNames,
                item = objectives.firstNotNullOfOrNull { it.display.item },
                decoration = CardTile.Decoration.MULTI_ITEM,
            )

            objective.display = objectiveDisplayService.resolve(objective.id, objective.data.display, display)
        }
    }

    override fun tick(card: BingoCard) {
        for (objective in card.objectivesByInstance<BingoObjective.AllOfEntry>()) {
            val objectives = objective.allOfObjectives.mapNotNull { card.objectives[it] }

            objectiveService.updateTeamsOnceAchieved(objective)

            objectiveService.updateTeamsSeen(objective) { team ->
                objectives.any { it.hasSeen(team) }
            }

            val capturesByPlayer = objectives
                .flatMap { it.playersHolding.entries }
                .groupBy { it.key }

            val progressTotal = objectives.size.coerceAtLeast(1).toFloat()

            val teamsProgress = objectiveService.getPlayersByTeam()
                .mapKeys { it.key.key }
                .mapValues { (_, players) ->
                    val progress = players.maxOfOrNull { capturesByPlayer[it.uuid]?.size ?: 0 } ?: 0
                    progress / progressTotal
                }

            val playersHolding = capturesByPlayer
                .filter { it.value.size == objectives.size } // in order to capture, must be holding every objective
                .mapValues { (_, captures) -> captures.map { it.value }.maxBy { it.instant } }

            val players = objectiveService.getPlayers(
                playersHolding = playersHolding
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