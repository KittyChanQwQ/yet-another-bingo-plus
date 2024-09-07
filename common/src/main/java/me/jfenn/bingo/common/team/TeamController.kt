package me.jfenn.bingo.common.team

import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.api.ITeamManager
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import net.minecraft.server.MinecraftServer

class TeamController(
    private val teamService: TeamService,
    private val state: BingoState,
    private val playerManager: IPlayerManager,
    private val teamManager: ITeamManager,
    private val server: MinecraftServer,
    events: ScopedEvents,
) {
    init {
        events.onEnter(GameState.PREGAME) {
            // clear the team of all online players when entering PREGAME
            // (so that they must rejoin a team for the next game)
            for (team in BingoTeam.TEAM_BLOCKS.values) {
                teamManager.deleteTeam(server, team.key.id)
            }
            state.teams.clear()
        }

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
                    teamService.onTeamChanged(player)
                    state.playersJoinedIds.add(player.uuid)
                }
            }
        }
    }
}