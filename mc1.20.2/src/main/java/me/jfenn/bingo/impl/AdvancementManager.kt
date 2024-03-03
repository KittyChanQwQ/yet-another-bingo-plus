package me.jfenn.bingo.impl

import me.jfenn.bingo.api.IAdvancementHandle
import me.jfenn.bingo.api.IAdvancementManager
import net.minecraft.advancement.AdvancementEntry
import net.minecraft.item.ItemStack
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.CommandOutput
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import kotlin.jvm.optionals.getOrNull

class AdvancementManager : IAdvancementManager {
    override fun getAdvancement(server: MinecraftServer, id: Identifier): IAdvancementHandle? {
        return server.advancementLoader.get(id)
            ?.let { AdvancementHandle(it) }
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
    val advancement: AdvancementEntry,
) : IAdvancementHandle {
    override val name: Text?
        get() = advancement.value.display.getOrNull()?.title
    override val description: Text?
        get() = advancement.value.display.getOrNull()?.description
    override val displayIcon: ItemStack?
        get() = advancement.value.display.getOrNull()?.icon
}