package me.jfenn.bingo.common.card.objective

import kotlinx.datetime.Clock
import me.jfenn.bingo.api.IPlayerHandle
import me.jfenn.bingo.api.IStatHandle
import me.jfenn.bingo.api.IStatManager
import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.CardGeneratorState
import me.jfenn.bingo.common.card.data.ObjectiveData
import me.jfenn.bingo.common.card.data.ObjectiveRegistryState
import me.jfenn.bingo.common.map.CardTile
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.BingoPlayerProfile
import me.jfenn.bingo.common.team.OfflinePlayerCache
import me.jfenn.bingo.common.utils.formatTitle
import net.minecraft.text.Text
import org.slf4j.Logger

class StatsObjectiveManager(
    private val log: Logger,
    private val statManager: IStatManager,
    private val state: BingoState,
    private val offlinePlayerCache: OfflinePlayerCache,
    private val objectiveRegistryState: ObjectiveRegistryState,
    private val objectiveDisplayService: ObjectiveDisplayService,
    private val objectiveService: ObjectiveService,
) : IObjectiveManager {
    override fun list(): Iterable<String> {
        return objectiveRegistryState.objectives
            .filterValues { it is ObjectiveData.Stats }
            .keys
    }

    override fun find(id: String, state: CardGeneratorState): BingoObjective? {
        val data = objectiveRegistryState.objectives[id] as? ObjectiveData.Stats
            ?: return null

        return BingoObjective.StatsEntry(
            id = id,
            data = data,
            valueMin = data.min.resolve(state),
            valueMax = data.max.resolve(state),
        )
    }

    private fun init(objective: BingoObjective): Boolean {
        if (objective !is BingoObjective.StatsEntry) return false

        val stat = statManager.getById(objective.data.statType, objective.data.statName)
            ?: return false

        objective.stat = stat

        val statNamespace = (objective.data.statName ?: objective.data.statType).substringBefore(':')
        val statTypePath = objective.data.statType.substringAfter(':')
        val statNamePath = objective.data.statName?.substringAfter(':')
        val statNameFallback = statTypePath.formatTitle() + (statNamePath?.let { " ${it.formatTitle()}" } ?: "")

        val statName = statNamePath
            // This translation key usually only works for 'minecraft:custom' stats
            ?.let { Text.translatableWithFallback("stat.$statNamespace.$it", statNameFallback) }
            ?: Text.literal(statNameFallback)

        val display = ObjectiveDisplay.Resolved(
            name = statName,
            decoration = CardTile.Decoration.ADVANCEMENT,
        )
        objective.display = objectiveDisplayService.resolve(objective.id, objective.data.display, display)
            .let {
                it.copy(name = Text.empty().append(it.name).append(": ${objective.valueMin}"))
            }

        return true
    }

    override fun init(card: BingoCard) {
        for (goal in card.objectivesByInstance<BingoObjective.StatsEntry>()) {
            if (!init(goal)) {
                log.error("[StatsObjectiveManager] Unable to find statistic ${goal.data.statType}/${goal.data.statName} for ${goal.id}")
            }
        }
    }

    private fun getStatValue(objective: BingoObjective.StatsEntry, stat: IStatHandle, player: IPlayerHandle): Int {
        val base = when {
            objective.data.relative -> {
                objective.baseStats.getOrPut(player.uuid) { stat.getForPlayer(player) }
            }
            else -> 0
        }
        return stat.getForPlayer(player) - base
    }

    override fun tick(card: BingoCard) {
        val now = Clock.System.now()

        for (objective in card.objectivesByInstance<BingoObjective.StatsEntry>()) {
            val stat = objective.stat ?: continue

            objectiveService.updateTeamsOnceAchieved(objective)

            val teamPlayers = state.getRegisteredTeams()
                .associate { it.key to it.players }
                .mapValues { (_, players) ->
                    players.map { offlinePlayerCache.getOfflinePlayer(it) }
                }

            val teamsProgress = teamPlayers.map { (team, players) ->
                val statProgress = players.maxOf { getStatValue(objective, stat, it) }
                    .takeIf { it <= objective.valueMax }
                    ?: 0
                val progress = statProgress / objective.valueMin.coerceAtLeast(1).toFloat()
                team to progress
            }.toMap()

            objectiveService.updateTeamsSeen(objective) { team ->
                teamPlayers[team]
                    ?.any { getStatValue(objective, stat, it) > 0 }
                    ?: false
            }

            val playersHolding = teamPlayers.asSequence()
                .flatMap { (team, players) -> players.map { team to it } } // List<(TeamKey, Player)>
                .filter { (_, player) -> getStatValue(objective, stat, player) in objective.valueRange }
                .associate { (team, player) ->
                    val capture = objective.playersHolding[player.uuid] ?: BingoObjectiveCapture(
                        team = team,
                        player = BingoPlayerProfile.fromPlayer(player),
                        instant = now,
                    )
                    player.uuid to capture
                }

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