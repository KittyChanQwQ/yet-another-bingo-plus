package me.jfenn.bingo.common.team

import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.api.ITeamHandle
import me.jfenn.bingo.api.ITeamManager
import me.jfenn.bingo.common.commands.CommandTreeHandler
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.text.Texts
import net.minecraft.util.Formatting
import kotlin.math.ceil

class TeamService(
    private val server: MinecraftServer,
    private val state: BingoState,
    private val teamManager: ITeamManager,
    private val commandTreeHandler: CommandTreeHandler,
    private val textProvider: TextProvider,
    private val playerManager: IPlayerManager
) {

    fun getPlayerTeam(player: ServerPlayerEntity): BingoTeam? {
        val team = teamManager.getPlayerTeam(player) ?: return null
        return state.teams[BingoTeamKey(team.name)]
    }

    fun isPlaying(player: ServerPlayerEntity): Boolean {
        return getPlayerTeam(player) != null
    }

    fun getTeamName(team: BingoTeam): Text {
        return textProvider.string(StringKey.TeamName, textProvider.string(team.name))
            .formatted(team.formatting)
            .let { Texts.bracketed(it) }
    }

    fun getTeamOrPlayerName(team: BingoTeam): Text {
        val player = team.players
            .takeIf { it.size == 1 }
            ?.first()
            ?: return getTeamName(team)

        return Text.literal(player.name)
            .formatted(team.formatting)
            .let { Texts.bracketed(it) }
    }

    fun createTeam(team: BingoTeam): ITeamHandle {
        return teamManager.createTeam(
            server,
            teamName = team.id,
            displayName = textProvider.string(team.name),
            color = team.formatting,
        )
    }

    fun joinTeam(player: ServerPlayerEntity, teamTemplate: BingoTeam) {
        if (getPlayerTeam(player)?.key == teamTemplate.key) return
        val team = state.registerTeam(teamTemplate)
        val playerProfile = BingoPlayerProfile.fromPlayer(playerManager.forPlayer(player))

        // Remove player from their existing team
        getPlayerTeam(player)
            ?.players
            ?.remove(playerProfile)

        teamManager.setPlayerTeam(player, createTeam(team))
        team.players.add(playerProfile)
        player.sendMessage(
            textProvider.string(StringKey.LobbyTeamChanged, getTeamName(team))
                .formatted(Formatting.YELLOW)
        )

        onTeamChanged(player)
    }

    fun onTeamChanged(player: ServerPlayerEntity) {
        // Send a new command tree once player is on a team
        commandTreeHandler.sendCommandTree(player)
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