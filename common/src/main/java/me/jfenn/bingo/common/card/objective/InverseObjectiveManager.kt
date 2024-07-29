package me.jfenn.bingo.common.card.objective

import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.CardGeneratorState
import me.jfenn.bingo.common.card.data.ObjectiveData
import me.jfenn.bingo.common.card.data.ObjectiveRegistryState
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text

class InverseObjectiveManager(
    private val state: BingoState,
    private val objectiveRegistryState: ObjectiveRegistryState,
    private val objectiveDisplayService: ObjectiveDisplayService,
    private val text: TextProvider,
) : IObjectiveManager {
    override fun find(id: String, state: CardGeneratorState): BingoObjective? {
        val data = objectiveRegistryState.objectives[id] as? ObjectiveData.Inverse
            ?: return null

        return BingoObjective.InverseEntry(
            id = id,
            inverseObjective = data.objective,
            permanent = data.permanent,
            displayData = data.display,
        )
    }

    override fun init(card: BingoCard) {
        for (objective in card.objectivesByInstance<BingoObjective.InverseEntry>()) {
            val display = card.objectives[objective.inverseObjective]?.let { inverse ->
                ObjectiveDisplay.Resolved(
                    name = text.string(StringKey.ObjectiveInverse, inverse.display.name ?: Text.empty()),
                    item = inverse.display.item,
                )
            } ?: ObjectiveDisplay.Resolved.EMPTY

            objective.display = objectiveDisplayService.resolve(objective.id, objective.displayData, display)
        }
    }

    override fun tick(card: BingoCard) {
        for (objective in card.objectivesByInstance<BingoObjective.InverseEntry>()) {
            val inverse = card.objectives[objective.inverseObjective] ?: continue

            for (team in state.getRegisteredTeams()) {
                val hasSeen = inverse.hasSeen(team.key)
                val hasAchieved = when {
                    // if permanent, a team should not be able to recapture this objective after losing it
                    objective.permanent -> !inverse.hasAchieved(team.key) && !objective.teamsOnceAchieved.contains(team.key)
                    else -> !inverse.hasAchieved(team.key)
                }

                if (hasSeen) inverse.markSeen(team.key)
                objective.setAchieved(team.key, hasAchieved)
            }
        }
    }
}