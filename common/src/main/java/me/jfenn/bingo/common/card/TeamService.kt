package me.jfenn.bingo.common.card

import me.jfenn.bingo.common.ChatMessages
import me.jfenn.bingo.api.ITeamManager
import me.jfenn.bingo.common.state.BingoContext
import net.minecraft.server.network.ServerPlayerEntity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.ceil

object TeamService : KoinComponent {

    private val teamManager by inject<ITeamManager>()

    fun joinTeam(player: ServerPlayerEntity, team: BingoTeam) {
        if (BingoTeam.fromPlayer(player) == team) return
        val server = player.server ?: return

        teamManager.setPlayerTeam(player, team.team(server))
        ChatMessages.broadcastJoinedTeam(player, team)
    }

    fun shuffleTeams(ctx: BingoContext, numTeams: Int) {
        val teams = BingoTeam.entries.shuffled()

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