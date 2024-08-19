package me.jfenn.bingo.api

import me.jfenn.bingo.common.scoreboard.ScoreChange
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

interface IScoreboardManager {

    fun createDummyObjective(server: MinecraftServer, name: String, displayName: Text): IObjectiveHandle

    fun setScoreboardText(server: MinecraftServer, handle: IObjectiveHandle, textLines: List<ScoreChange.Create>)

    fun sendObjectiveCreate(player: ServerPlayerEntity, handle: IObjectiveHandle)

    fun sendObjectiveDelete(player: ServerPlayerEntity, handle: IObjectiveHandle)

    fun sendScoreChanges(player: ServerPlayerEntity, handle: IObjectiveHandle, changes: List<ScoreChange>)

    fun getPlayerName(player: ServerPlayerEntity): String

    fun getByName(server: MinecraftServer, name: String): IObjectiveHandle?

}

interface IObjectiveHandle {
    val name: String
    fun getForPlayer(player: IPlayerHandle): Int?
}
