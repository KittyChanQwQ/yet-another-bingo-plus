package me.jfenn.bingo.common.team

import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.state.BingoState
import net.minecraft.server.MinecraftServer

class TeamController(
    private val teamService: TeamService,
    private val server: MinecraftServer,
    private val state: BingoState,
    private val playerManager: IPlayerManager,
    events: ScopedEvents,
) {
    init {
        events.onGameTick {
            for (player in playerManager.getPlayers()) {
                val playerTeam = teamService.getPlayerTeam(player.player)

                // If the player has switched teams, update the player UUID sets...
                // (this is necessary because teams could be changed without us knowing, using /team join)
                val playerProfile = BingoPlayerProfile.fromPlayer(player)
                val isTeamLeft = state.teams.values
                    .filter { it.key != playerTeam?.key }
                    .any { team ->
                        team.players.remove(playerProfile)
                    }

                val isTeamJoined = playerTeam?.players?.add(playerProfile) ?: false

                if (isTeamLeft || isTeamJoined) {
                    teamService.onTeamChanged(player.player)
                    state.playersJoinedIds.add(player.uuid)
                }
            }
        }
    }
}