package me.jfenn.bingo.common.card

import me.jfenn.bingo.api.ITeamManager
import me.jfenn.bingo.common.Bingo
import me.jfenn.bingo.common.ChatMessages
import me.jfenn.bingo.common.state.BingoContext
import net.minecraft.server.network.ServerPlayerEntity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.ceil

object TeamService : KoinComponent {

    private val teamManager by inject<ITeamManager>()

    fun joinTeam(player: ServerPlayerEntity, teamTemplate: BingoTeam) {
        val ctx = Bingo.getContext(player.server) ?: return
        if (BingoTeam.fromPlayer(player)?.key == teamTemplate.key) return
        val server = player.server ?: return
        val team = ctx.state.registerTeam(teamTemplate)
        val playerProfile = BingoPlayerProfile.fromPlayer(player)

        // Remove player from their existing team
        BingoTeam.fromPlayer(player)
            ?.players
            ?.remove(playerProfile)

        teamManager.setPlayerTeam(player, team.team(server))
        team.players.add(playerProfile)
        ChatMessages.broadcastJoinedTeam(player, team)
    }

    fun shuffleTeams(ctx: BingoContext, numTeams: Int) {
        val teams = BingoTeam.TEAM_BLOCKS.values.shuffled()

        val players = ctx.server.playerManager.playerList
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