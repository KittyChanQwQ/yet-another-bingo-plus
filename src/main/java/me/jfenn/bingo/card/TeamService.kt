package me.jfenn.bingo.card

import me.jfenn.bingo.minecraftServer
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import kotlin.math.ceil

object TeamService {

    fun joinTeam(player: PlayerEntity, team: BingoTeam) {
        if (player.isTeamPlayer(team.team())) return
        minecraftServer.scoreboard.addPlayerToTeam(player.entityName, team.team())

        player.sendMessage(
            Text.empty()
                .append(Text.literal("➾  You have been moved to ").formatted(Formatting.YELLOW))
                .append(team.text())
        )
    }

    fun shuffleTeams(numTeams: Int) {
        val teams = BingoTeam.entries

        val players = minecraftServer.playerManager.playerList
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