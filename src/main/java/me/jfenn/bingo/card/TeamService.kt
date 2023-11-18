package me.jfenn.bingo.card

import me.jfenn.bingo.ChatMessages
import me.jfenn.bingo.state.BingoContext
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import kotlin.math.ceil

object TeamService {

    fun joinTeam(player: ServerPlayerEntity, team: BingoTeam) {
        if (BingoTeam.fromPlayer(player) == team) return
        val server = player.server ?: return
        server.scoreboard.addPlayerToTeam(player.entityName, team.team(server))
        ChatMessages.broadcastJoinedTeam(player, team)
    }

    fun shuffleTeams(ctx: BingoContext, numTeams: Int) {
        val teams = BingoTeam.entries

        val players = ctx.server.playerManager.playerList
        val playersPerTeam = ceil(players.size / numTeams.coerceIn(1, teams.size).toDouble()).toInt()

        players.shuffled()
            .chunked(playersPerTeam)
            .forEachIndexed { i, teamPlayers ->
                val team = teams[i % teams.size]
                for (teamPlayer in teamPlayers)
                    joinTeam(teamPlayer, team)
            }
    }

}