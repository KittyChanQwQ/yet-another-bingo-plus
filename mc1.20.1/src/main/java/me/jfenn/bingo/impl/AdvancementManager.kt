package me.jfenn.bingo.impl

import me.jfenn.bingo.platform.IAdvancementHandle
import me.jfenn.bingo.platform.IAdvancementManager
import net.minecraft.advancement.Advancement
import net.minecraft.item.ItemStack
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.CommandOutput
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class AdvancementManager : IAdvancementManager {
    override fun listAdvancements(server: MinecraftServer): List<String> {
        return server.advancementLoader.advancements
            .filter { !it.id.path.endsWith("/root") && it.display != null }
            .map { it.id.toString() }
    }

    override fun getAdvancement(server: MinecraftServer, id: String): IAdvancementHandle? {
        return server.advancementLoader.get(Identifier(id))
            ?.let { AdvancementHandle(it) }
    }

    override fun getProgress(player: ServerPlayerEntity, advancement: IAdvancementHandle): Float {
        require(advancement is AdvancementHandle)
        return player.advancementTracker.getProgress(advancement.advancement).progressBarPercentage
    }

    override fun isAnyObtained(player: ServerPlayerEntity, advancement: IAdvancementHandle): Boolean {
        require(advancement is AdvancementHandle)
        return player.advancementTracker.getProgress(advancement.advancement).isAnyObtained
    }

    override fun isDone(player: ServerPlayerEntity, advancement: IAdvancementHandle): Boolean {
        require(advancement is AdvancementHandle)
        return player.advancementTracker.getProgress(advancement.advancement).isDone
    }

    override fun clearAdvancements(player: ServerPlayerEntity) {
        // The yarn mappings for these APIs conflict between versions, so we're using the command for this instead
        player.server.commandManager.executeWithPrefix(player.server.commandSource.withOutput(CommandOutput.DUMMY), "advancement revoke ${player.entityName} everything")
    }
}

class AdvancementHandle(
    val advancement: Advancement,
) : IAdvancementHandle {
    override val name: Text?
        get() = advancement.display?.title
    override val description: Text?
        get() = advancement.display?.description
    override val displayIcon: ItemStack?
        get() = advancement.display?.icon
}