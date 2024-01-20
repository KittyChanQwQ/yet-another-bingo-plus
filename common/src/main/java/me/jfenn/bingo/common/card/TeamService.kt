package me.jfenn.bingo.common.card

import me.jfenn.bingo.api.ITeamHandle
import me.jfenn.bingo.api.ITeamManager
import me.jfenn.bingo.common.ChatMessages
import me.jfenn.bingo.common.state.BingoState
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import kotlin.math.ceil

class TeamService(
    private val server: MinecraftServer,
    private val state: BingoState,
    private val teamManager: ITeamManager,
) {

    fun getPlayerTeam(player: ServerPlayerEntity): BingoTeam? {
        val team = teamManager.getPlayerTeam(player) ?: return null
        return state.teams[BingoTeamKey(team.name)]
    }

    fun createTeam(team: BingoTeam): ITeamHandle {
        return teamManager.createTeam(
            server,
            teamName = team.id,
            displayName = Text.of(team.name.substring(0, 1) + team.name.substring(1).lowercase()),
            color = team.formatting,
        )
    }

    fun joinTeam(player: ServerPlayerEntity, teamTemplate: BingoTeam) {
        if (getPlayerTeam(player)?.key == teamTemplate.key) return
        val team = state.registerTeam(teamTemplate)
        val playerProfile = BingoPlayerProfile.fromPlayer(player)

        // Remove player from their existing team
        getPlayerTeam(player)
            ?.players
            ?.remove(playerProfile)

        teamManager.setPlayerTeam(player, createTeam(team))
        team.players.add(playerProfile)
        ChatMessages.broadcastJoinedTeam(player, team)
    }

    fun shuffleTeams(numTeams: Int) {
        val teams = BingoTeam.TEAM_BLOCKS.values.shuffled()

        val players = server.playerManager.playerList
        val playersPerTeam = ceil(players.size / numTeams.coerceIn(1, teams.size).toDouble()).toInt()

        players.shuffled()
            .chunked(playersPerTeam)
            .forEachIndexed { i, teamPlayers ->
                val teamTemplate = teams[i % teams.size]
                for (teamPlayer in teamPlayers)
                    joinTeam(teamPlayer, teamTemplate)
            }
    }

}