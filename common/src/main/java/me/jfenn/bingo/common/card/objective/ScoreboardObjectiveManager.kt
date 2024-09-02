package me.jfenn.bingo.common.card.objective

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jfenn.bingo.api.IObjectiveHandle
import me.jfenn.bingo.api.IPlayerHandle
import me.jfenn.bingo.api.IScoreboardManager
import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.CardGeneratorState
import me.jfenn.bingo.common.card.data.ObjectiveData
import me.jfenn.bingo.common.card.data.ObjectiveRegistryState
import me.jfenn.bingo.common.card.objective.ObjectiveDisplay.Companion.FORMAT_MAX
import me.jfenn.bingo.common.card.objective.ObjectiveDisplay.Companion.FORMAT_MIN
import me.jfenn.bingo.common.map.CardTile
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.BingoPlayerProfile
import me.jfenn.bingo.common.team.OfflinePlayerCache
import me.jfenn.bingo.common.utils.formatTitle
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import org.slf4j.Logger

class ScoreboardObjectiveManager(
    private val log: Logger,
    private val server: MinecraftServer,
    private val scoreboardManager: IScoreboardManager,
    private val state: BingoState,
    private val offlinePlayerCache: OfflinePlayerCache,
    private val objectiveRegistryState: ObjectiveRegistryState,
    private val objectiveDisplayService: ObjectiveDisplayService,
    private val objectiveService: ObjectiveService,
) : IObjectiveManager {
    override fun list(): Iterable<String> {
        return objectiveRegistryState.objectives
            .filterValues { it is ObjectiveData.Scoreboard }
            .keys
    }

    override fun find(id: String, state: CardGeneratorState): BingoObjective? {
        val data = objectiveRegistryState.objectives[id] as? ObjectiveData.Scoreboard
            ?: return null

        return BingoObjective.ScoreboardEntry(
            id = id,
            data = data,
            valueMin = data.min.resolve(state),
            valueMax = data.max.resolve(state),
        )
    }

    private fun init(objective: BingoObjective): Boolean {
        if (objective !is BingoObjective.ScoreboardEntry) return false

        val scoreboard = scoreboardManager.getByName(server, objective.data.scoreboardName)
            ?: return false

        objective.scoreboard = scoreboard

        val display = ObjectiveDisplay.Resolved(
            name = Text.literal(objective.data.scoreboardName.formatTitle())
                .append(": $FORMAT_MIN"),
            decoration = CardTile.Decoration.ADVANCEMENT,
        )
        objective.display = objectiveDisplayService.resolve(
            id = objective.id,
            data = objective.data.display,
            fallback = display,
            substitutions = mapOf(
                FORMAT_MIN to objective.valueMin.toString(),
                FORMAT_MAX to objective.valueMax.toString(),
            )
        )

        return true
    }

    override fun init(card: BingoCard) {
        for (goal in card.objectivesByInstance<BingoObjective.ScoreboardEntry>()) {
            if (!init(goal)) {
                log.error("[StatsObjectiveManager] Unable to find scoreboard ${goal.data.scoreboardName} for ${goal.id}")
            }
        }
    }

    private fun getScoreboardValue(objective: BingoObjective.ScoreboardEntry, scoreboard: IObjectiveHandle, player: IPlayerHandle): Int {
        val base = when {
            objective.data.relative -> {
                objective.baseScores.getOrPut(player.uuid) { scoreboard.getForPlayer(player) ?: 0 }
            }
            else -> 0
        }

        val score = scoreboard.getForPlayer(player) ?: 0
        return score - base
    }

    override fun tick(card: BingoCard) {
        val now = state.updatedAt ?: Instant.DISTANT_PAST

        for (objective in card.objectivesByInstance<BingoObjective.ScoreboardEntry>()) {
            val scoreboard = objective.scoreboard ?: continue

            objectiveService.updateTeamsOnceAchieved(objective)

            val teamPlayers = state.getRegisteredTeams()
                .associate { it.key to it.players }
                .mapValues { (_, players) ->
                    players.map { offlinePlayerCache.getOfflinePlayer(it) }
                }

            val teamsProgress = teamPlayers.map { (team, players) ->
                val scoreboardProgress = players.maxOf { getScoreboardValue(objective, scoreboard, it) }
                    .takeIf { it <= objective.valueMax }
                    ?: 0
                val progress = scoreboardProgress / objective.valueMin.coerceAtLeast(1).toFloat()
                team to progress
            }.toMap()

            objectiveService.updateTeamsSeen(objective) { team ->
                teamPlayers[team]
                    ?.any { getScoreboardValue(objective, scoreboard, it) > 0 }
                    ?: false
            }

            val playersHolding = teamPlayers.asSequence()
                .flatMap { (team, players) -> players.map { team to it } } // List<(TeamKey, Player)>
                .filter { (_, player) -> getScoreboardValue(objective, scoreboard, player) in objective.valueRange }
                .associate { (team, player) ->
                    val capture = objective.playersHolding[player.uuid] ?: BingoObjectiveCapture(
                        team = team,
                        player = BingoPlayerProfile.fromPlayer(player),
                        instant = now,
                    )
                    player.uuid to capture
                }

            val players = objectiveService.getPlayers(
                playersHolding = playersHolding,
                players = objective.players,
                shouldRetainCaptures = false,
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