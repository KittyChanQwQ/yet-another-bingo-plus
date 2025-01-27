package me.jfenn.bingo.platform

import me.jfenn.bingo.platform.item.IItemStack
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

interface IAdvancementManager {

    fun listAdvancements(server: MinecraftServer): List<String>

    fun getAdvancement(server: MinecraftServer, id: String): IAdvancementHandle?

    fun getProgress(player: ServerPlayerEntity, advancement: IAdvancementHandle): Float

    fun isAnyObtained(player: ServerPlayerEntity, advancement: IAdvancementHandle): Boolean

    fun isDone(player: ServerPlayerEntity, advancement: IAdvancementHandle): Boolean

    fun clearAdvancements(player: ServerPlayerEntity)

}

interface IAdvancementHandle {
    val name: Text?
    val description: Text?
    val displayIcon: IItemStack?
}
