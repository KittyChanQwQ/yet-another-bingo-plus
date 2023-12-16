package me.jfenn.bingo.api

import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting

interface ITeamManager {

    fun createTeam(server: MinecraftServer, teamName: String, displayName: Text, color: Formatting): ITeamHandle

    fun getPlayerTeam(player: ServerPlayerEntity): ITeamHandle?

    fun setPlayerTeam(player: ServerPlayerEntity, team: ITeamHandle?)

}

interface ITeamHandle {
    val name: String
    val playerList: List<String>
}
