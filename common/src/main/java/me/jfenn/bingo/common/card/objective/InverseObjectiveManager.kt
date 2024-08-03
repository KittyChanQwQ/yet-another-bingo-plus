package me.jfenn.bingo.common.card.objective

import kotlinx.datetime.Instant
import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.CardGeneratorState
import me.jfenn.bingo.common.card.data.ObjectiveData
import me.jfenn.bingo.common.card.data.ObjectiveRegistryState
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.BingoPlayerProfile
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text

class InverseObjectiveManager(
    private val state: BingoState,
    private val objectiveRegistryState: ObjectiveRegistryState,
    private val objectiveDisplayService: ObjectiveDisplayService,
    private val objectiveService: ObjectiveService,
    private val text: TextProvider,
) : IObjectiveManager {
    override fun list(): Iterable<String> {
        return objectiveRegistryState.objectives
            .filterValues { it is ObjectiveData.Inverse }
            .keys
    }

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

            objectiveService.updateTeamsOnceAchieved(objective)

            if (objective.permanent) {
                // once a player earns the inverse objective, they can never recapture this one
                objective.playersOnceAchieved.addAll(inverse.playersHolding.keys)
            }

            val playersHolding = objectiveService.getTeamPlayers()
                .filter { (_, player) -> !inverse.playersHolding.containsKey(player.uuid) }
                .filter { (_, player) ->
                    !objective.playersOnceAchieved.contains(player.uuid)
                }
                .associateBy(
                    { (_, player) -> player.uuid },
                    { (team, player) ->
                        BingoObjectiveCapture(
                            team = team.key,
                            player = BingoPlayerProfile.fromPlayer(player),
                            instant = state.startedAt ?: Instant.DISTANT_PAST
                        )
                    }
                )

            objective.teamsSeen = inverse.teamsSeen

            val players = objective.playersHolding

            val teams = objective.playersHolding.map { it.value.team }.toMutableSet()

            objectiveService.update(
                objective = objective,
                playersHolding = playersHolding,
                players = players,
                teams = teams
            )
        }
    }
}