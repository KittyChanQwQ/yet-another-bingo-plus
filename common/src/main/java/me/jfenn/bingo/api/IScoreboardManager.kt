package me.jfenn.bingo.api

import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

interface IScoreboardManager {

    fun createDummyObjective(server: MinecraftServer, name: String, displayName: Text): IObjectiveHandle

    fun setScoreboardText(server: MinecraftServer, handle: IObjectiveHandle, textLines: List<String>)

    fun sendScoreboardDisplay(player: ServerPlayerEntity, handle: IObjectiveHandle?)

    fun sendScoreboardUpdates(player: ServerPlayerEntity, handle: IObjectiveHandle)

}

interface IObjectiveHandle {
    val name: String
}
