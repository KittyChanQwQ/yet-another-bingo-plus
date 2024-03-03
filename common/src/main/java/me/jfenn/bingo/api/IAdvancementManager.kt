package me.jfenn.bingo.api

import net.minecraft.item.ItemStack
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier

interface IAdvancementManager {

    fun getAdvancement(server: MinecraftServer, id: Identifier): IAdvancementHandle?

    fun isAnyObtained(player: ServerPlayerEntity, advancement: IAdvancementHandle): Boolean

    fun isDone(player: ServerPlayerEntity, advancement: IAdvancementHandle): Boolean

    fun clearAdvancements(player: ServerPlayerEntity)

}

interface IAdvancementHandle {
    val name: Text?
    val description: Text?
    val displayIcon: ItemStack?
}
