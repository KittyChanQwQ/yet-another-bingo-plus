package me.jfenn.bingo.platform

import me.jfenn.bingo.platform.text.Formatting
import me.jfenn.bingo.platform.text.IText
import net.minecraft.server.level.ServerPlayer

interface ITeamManager {

    fun listTeams(): List<String>

    fun getTeam(id: String): ITeamHandle?

    fun createTeam(teamName: String, displayName: IText, color: Formatting): ITeamHandle

    fun deleteTeam(teamName: String)

    fun getPlayerTeam(player: ServerPlayer): ITeamHandle?

    fun setPlayerTeam(player: ServerPlayer, team: ITeamHandle?)

}

interface ITeamHandle {
    val name: String
    var displayName: IText
    var color: Formatting?
}
