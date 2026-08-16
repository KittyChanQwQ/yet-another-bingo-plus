package me.jfenn.bingo.impl

import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.ITeamHandle
import me.jfenn.bingo.platform.ITeamManager
import me.jfenn.bingo.platform.text.Formatting
import me.jfenn.bingo.platform.text.IText
import net.minecraft.ChatFormatting
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.scores.PlayerTeam

class TeamManager(
    private val server: MinecraftServer,
) : ITeamManager {

    override fun listTeams(): List<String> {
        return server.scoreboard.teamNames.toList()
    }

    override fun getTeam(id: String): ITeamHandle? {
        return server.scoreboard.getPlayerTeam(id)
            ?.let { TeamHandle(it) }
    }

    override fun createTeam(teamName: String, displayName: IText, color: Formatting): ITeamHandle {
        return TeamHandle(
            server.scoreboard.getPlayerTeam(teamName)
                ?: server.scoreboard.addPlayerTeam(teamName)
        ).also {
            it.displayName = displayName
            it.color = color
        }
    }

    override fun deleteTeam(teamName: String) {
        val team = server.scoreboard.getPlayerTeam(teamName)
        if (team != null) server.scoreboard.removePlayerTeam(team)
    }

    override fun getPlayerTeam(player: IPlayerHandle): TeamHandle? {
        val player: ServerPlayer = player.player
        return server.scoreboard.getPlayersTeam(player.scoreboardName)
            ?.let { TeamHandle(it) }
    }

    override fun setPlayerTeam(player: IPlayerHandle, team: ITeamHandle?) {
        require(team is TeamHandle?)
        val serverPlayer: ServerPlayer = player.player

        if (team != null) {
            server.scoreboard.addPlayerToTeam(serverPlayer.scoreboardName, team.team)
        } else {
            // leave the player's current team (set to null)
            val currentTeam = getPlayerTeam(player) ?: return
            server.scoreboard.removePlayerFromTeam(serverPlayer.scoreboardName, currentTeam.team)
        }
    }
}

class TeamHandle(
    val team: PlayerTeam
) : ITeamHandle {
    override val name: String
        get() = team.name

    override var displayName: IText
        get() = TextImpl(team.displayName.copy())
        set(value) { team.displayName = value.value }

    override var color: Formatting?
        get() = team.color.toPlatformFormatting()
        set(value) { team.color = value?.toMinecraftFormatting() ?: ChatFormatting.WHITE }
}
