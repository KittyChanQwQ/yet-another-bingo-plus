package me.jfenn.bingo.common.card.objective

import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.CardGeneratorState
import me.jfenn.bingo.common.card.data.ObjectiveData
import me.jfenn.bingo.common.card.data.ObjectiveRegistryState
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text
import net.minecraft.util.Formatting

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
            displayData = data.display,
        )
    }

    override fun init(card: BingoCard) {
        for (objective in card.objectivesByInstance<BingoObjective.InverseEntry>()) {
            objective.display = objective.displayData
                ?.let { objectiveDisplayService.resolve(objective.id, it) }
                ?: run {
                    card.objectives[objective.inverseObjective]?.let { objective ->
                        ObjectiveDisplay.Resolved(
                            name = text.string(StringKey.ObjectiveInverse, objective.display.name ?: Text.empty())
                                .formatted(Formatting.GRAY, Formatting.ITALIC),
                            item = objective.display.item,
                        )
                    } ?: ObjectiveDisplay.Resolved.EMPTY
                }
        }
    }

    override fun tick(card: BingoCard) {
        for (objective in card.objectivesByInstance<BingoObjective.InverseEntry>()) {
            val inverse = card.objectives[objective.inverseObjective] ?: continue

            for (team in state.getRegisteredTeams()) {
                val hasSeen = inverse.hasSeen(team.key)
                val hasAchieved = !inverse.hasAchieved(team.key) && !objective.teamsOnceAchieved.contains(team.key)

                if (hasSeen) inverse.markSeen(team.key)
                objective.setAchieved(team.key, hasAchieved)

                // Once the inverse tile is lost, add the team to `teamsOnceAchieved` so they cannot obtain it again
                if (!hasAchieved) objective.teamsOnceAchieved.add(team.key)
            }
        }
    }
}