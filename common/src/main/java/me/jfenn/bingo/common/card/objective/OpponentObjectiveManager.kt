package me.jfenn.bingo.common.card.objective

import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.CardGeneratorState
import me.jfenn.bingo.common.card.data.ObjectiveData
import me.jfenn.bingo.common.card.data.ObjectiveRegistryState
import me.jfenn.bingo.common.map.CardTile
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text

class OpponentObjectiveManager(
    private val objectiveRegistryState: ObjectiveRegistryState,
    private val objectiveDisplayService: ObjectiveDisplayService,
    private val objectiveService: ObjectiveService,
    private val text: TextProvider,
) : IObjectiveManager {
    override fun list(): Iterable<String> {
        return objectiveRegistryState.rootObjectives
            .filter { (_, it) -> it is ObjectiveData.Opponent }
            .map { it.key }
            .toList()
    }

    override fun find(id: String, state: CardGeneratorState): BingoObjective? {
        val data = objectiveRegistryState.objectives[id] as? ObjectiveData.Opponent
            ?: return null

        return BingoObjective.OpponentEntry(
            id = id,
            data = data,
            opponentObjective = data.expandReference(id, data.objective),
        )
    }

    override fun init(card: BingoCard) {
        for (objective in card.objectivesByInstance<BingoObjective.OpponentEntry>()) {
            val display = card.objectives[objective.opponentObjective]?.let { opponent ->
                ObjectiveDisplay.Resolved(
                    name = text.string(StringKey.ObjectiveOpponent, opponent.display.name ?: Text.empty()),
                    item = opponent.display.item,
                    decoration = CardTile.Decoration.ADVANCEMENT,
                )
            } ?: ObjectiveDisplay.Resolved.EMPTY

            objective.display = objectiveDisplayService.resolve(objective.id, objective.data.display, display)
        }
    }

    override fun tick(card: BingoCard) {
        for (objective in card.objectivesByInstance<BingoObjective.OpponentEntry>()) {
            val opponent = card.objectives[objective.opponentObjective] ?: continue

            objectiveService.updateTeamsOnceAchieved(objective)

            objective.teamsSeen = opponent.teamsSeen

            val teams = objectiveService.getAllTeams()
                .filter { team -> opponent.playersHolding.values.any { it.team != team.key } }
                .map { it.key }
                .toSet()
                .let {
                    // once an opponent earns the objective, they can never un-capture this one
                    if (objective.permanent) it.plus(objective.teamsOnceAchieved) else it
                }

            objectiveService.update(
                objective = objective,
                teams = teams
            )
        }
    }
}
