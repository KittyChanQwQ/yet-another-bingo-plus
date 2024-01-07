package me.jfenn.bingo.impl

import me.jfenn.bingo.api.ITeamHandle
import me.jfenn.bingo.api.ITeamManager
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.scoreboard.Team
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class TeamManager : ITeamManager {

    override fun createTeam(server: MinecraftServer, teamName: String, displayName: Text, color: Formatting): ITeamHandle {
        return TeamHandle(
            server.scoreboard.getTeam(teamName)
                ?: server.scoreboard.addTeam(teamName).also {
                    it.displayName = displayName
                    it.color = color
                }
        )
    }

    override fun getPlayerTeam(player: ServerPlayerEntity): TeamHandle? {
        return player.server.scoreboard.getPlayerTeam(player.entityName)
            ?.let { TeamHandle(it) }
    }

    override fun setPlayerTeam(player: ServerPlayerEntity, team: ITeamHandle?) {
        require(team is TeamHandle?)

        if (team != null) {
            player.server.scoreboard.addPlayerToTeam(player.entityName, team.team)
        } else {
            // leave the player's current team (set to null)
            val currentTeam = getPlayerTeam(player) ?: return
            player.server.scoreboard.removePlayerFromTeam(player.entityName, currentTeam.team)
        }
    }
}

class TeamHandle(
    val team: Team
) : ITeamHandle {
    override val name: String
        get() = team.name

    override val playerList: List<String>
        get() = team.playerList.toList()
}
