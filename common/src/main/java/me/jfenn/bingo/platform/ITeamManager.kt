package me.jfenn.bingo.platform

import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting

interface ITeamManager {

    fun listTeams(): List<String>

    fun getTeam(id: String): ITeamHandle?

    fun createTeam(teamName: String, displayName: Text, color: Formatting): ITeamHandle

    fun deleteTeam(teamName: String)

    fun getPlayerTeam(player: ServerPlayerEntity): ITeamHandle?

    fun setPlayerTeam(player: ServerPlayerEntity, team: ITeamHandle?)

}

interface ITeamHandle {
    val name: String
    var displayName: Text
    var color: Formatting
}
