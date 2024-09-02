package me.jfenn.bingo.common.card.objective

import kotlinx.datetime.Instant
import me.jfenn.bingo.api.IPlayerHandle
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.team.OfflinePlayerCache
import me.jfenn.bingo.common.utils.toMap
import java.util.*

class ObjectiveService(
    private val state: BingoState,
    private val options: BingoOptions,
    private val offlinePlayerCache: OfflinePlayerCache,
) {

    fun getTeamPlayers(): List<Pair<BingoTeam, IPlayerHandle>> = buildList {
        for (team in state.getRegisteredTeams()) {
            for (player in team.players) {
                add(Pair(team, offlinePlayerCache.getOfflinePlayer(player)))
            }
        }
    }

    fun getPlayersByTeam(): Map<BingoTeam, List<IPlayerHandle>> {
        return state.getRegisteredTeams()
            .associateWith { team ->
                team.players.map { offlinePlayerCache.getOfflinePlayer(it) }
            }
    }

    fun getLockedTeams() = state.getRegisteredTeams().filter { it.isWinner() }

    /**
     * Updates components of the objective state, but ensures that
     * values do not change for any teams that should not be updated
     */
    fun update(
        objective: BingoObjective,
        playersHolding: Map<UUID, BingoObjectiveCapture>? = null,
        players: Map<UUID, BingoObjectiveCapture>? = null,
        teamsProgress: Map<BingoTeamKey, Float>? = null,
        teams: Set<BingoTeamKey>? = null,
    ) {
        val lockedTeams = getLockedTeams()
        val lockedTeamKeys = lockedTeams.map { it.key }.toSet()

        if (playersHolding != null) {
            val newPlayersHolding = playersHolding.toMutableMap()

            newPlayersHolding.values
                .filter { lockedTeamKeys.contains(it.team) }
                .onEach { newPlayersHolding.remove(it.player.uuid) }

            objective.playersHolding.entries
                .filter { lockedTeamKeys.contains(it.value.team) }
                .toMap()
                .let { newPlayersHolding.putAll(it) }

            objective.playersHolding = newPlayersHolding
        }

        if (players != null) {
            val newPlayers = players.toMutableMap()

            newPlayers.values
                .filter { lockedTeamKeys.contains(it.team) }
                .onEach { newPlayers.remove(it.player.uuid) }

            objective.players.entries
                .filter { lockedTeamKeys.contains(it.value.team) }
                .toMap()
                .let { newPlayers.putAll(it) }

            objective.players = newPlayers
        }

        if (teamsProgress != null) {
            val newTeamsProgress = teamsProgress.toMutableMap()
            lockedTeamKeys.forEach { newTeamsProgress.remove(it) }
            newTeamsProgress.putAll(objective.teamsProgress.filterKeys { lockedTeamKeys.contains(it) })
            objective.teamsProgress = newTeamsProgress
        }

        if (teams != null) {
            val now = state.updatedAt ?: Instant.DISTANT_PAST
            val newTeamsAchieved = teams
                .associateWith { objective.teamsAchieved[it] ?: now }
                .toMutableMap()

            val newTeamsLost = (objective.teamsAchieved.keys + objective.teamsLost.keys)
                .filter { !newTeamsAchieved.containsKey(it) }
                .associateWith { objective.teamsLost[it] ?: now }
                .toMutableMap()

            newTeamsAchieved.keys
                .filter { lockedTeamKeys.contains(it) }
                .onEach { newTeamsAchieved.remove(it) }

            newTeamsLost.keys
                .filter { lockedTeamKeys.contains(it) }
                .onEach { newTeamsLost.remove(it) }

            objective.teamsAchieved.entries
                .filter { lockedTeamKeys.contains(it.key) }
                .toMap()
                .let { newTeamsAchieved.putAll(it) }

            objective.teamsLost.entries
                .filter { lockedTeamKeys.contains(it.key) }
                .toMap()
                .let { newTeamsLost.putAll(it) }

            objective.teamsAchieved = newTeamsAchieved
            objective.teamsLost = newTeamsLost
        }
    }

    fun getPlayers(
        playersHolding: Map<UUID, BingoObjectiveCapture>,
        players: Map<UUID, BingoObjectiveCapture>,
        shouldRetainCaptures: Boolean = !options.isInventoryMode,
    ): Map<UUID, BingoObjectiveCapture> {
        // Once captured, most objectives can only be lost if isInventoryMode=true
        // therefore, combine both players & playersHolding before updating the objective
        val combinedPlayersHolding = when {
            shouldRetainCaptures -> playersHolding + players
            else -> playersHolding
        }

        return when {
            options.isLockoutMode -> {
                // oldest capture first
                val teamHolding = combinedPlayersHolding.minByOrNull { it.value.instant }
                    ?.value?.team
                    ?.let { state.teams[it] }

                if (teamHolding != null)
                    combinedPlayersHolding.filter { teamHolding.players.contains(it.value.player) }.toMutableMap()
                else mutableMapOf()
            }
            else -> combinedPlayersHolding.entries
                .sortedBy { it.value.instant } // oldest captures first
                .distinctBy { it.value.team }
                .toMap()
        }
    }

    fun getTeams(
        players: Map<UUID, BingoObjectiveCapture>,
    ): Set<BingoTeamKey> {
        return state.getRegisteredTeams()
            .filter { team -> team.players.any { players.containsKey(it.uuid) } }
            .map { it.key }
            .toSet()
    }

    fun updateTeamsOnceAchieved(objective: BingoObjective) {
        objective.teamsOnceAchieved.addAll(objective.teamsAchieved.keys)
    }

    fun updateTeamsSeen(
        objective: BingoObjective,
        condition: (BingoTeamKey) -> Boolean
    ) {
        // This can optimize condition checks a little, because
        // teamsSeen is only ever additive
        // (we don't need to check teams again once they're in the set)

        val teamsNotSeen = state.getRegisteredTeams().map { it.key }.toSet() - objective.teamsSeen
        objective.teamsSeen.addAll(teamsNotSeen.filter(condition))
    }

}