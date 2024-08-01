package me.jfenn.bingo.common.card.objective

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

    fun getPlayers(
        objective: BingoObjective
    ): MutableMap<UUID, BingoObjectiveCapture> {
        val playersHolding = objective.playersHolding

        return when {
            options.isLockoutMode -> {
                // oldest capture first
                val teamHolding = playersHolding.minByOrNull { it.value.instant }
                    ?.value?.team
                    ?.let { state.teams[it] }

                if (teamHolding != null)
                    playersHolding.filter { teamHolding.players.contains(it.value.player) }.toMutableMap()
                else mutableMapOf()
            }
            else -> playersHolding.entries
                .let { entries ->
                    when {
                        options.isInventoryMode -> entries
                        else -> entries + objective.players.entries
                    }
                }
                .sortedBy { it.value.instant } // oldest captures first
                .distinctBy { it.value.team }
                .toMap()
                .toMutableMap()
        }
    }

    fun getTeams(
        objective: BingoObjective
    ): MutableSet<BingoTeamKey> {
        val players = objective.players

        return state.getRegisteredTeams()
            .filter { team -> team.players.any { players.containsKey(it.uuid) } }
            .map { it.key }
            .toMutableSet()
    }

    fun getTeamsSeen(
        objective: BingoObjective,
        condition: (BingoTeamKey) -> Boolean
    ): MutableSet<BingoTeamKey> {
        // This can optimize condition checks a little, because
        // teamsSeen is only ever additive
        // (we don't need to check teams again once they're in the set)

        val teamsNotSeen = state.getRegisteredTeams().map { it.key }.toSet() - objective.teamsSeen
        objective.teamsSeen.addAll(teamsNotSeen.filter(condition))
        return objective.teamsSeen
    }

}