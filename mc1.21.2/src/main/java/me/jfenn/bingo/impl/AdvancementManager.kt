package me.jfenn.bingo.impl

import me.jfenn.bingo.api.IAdvancementHandle
import me.jfenn.bingo.api.IAdvancementManager
import net.minecraft.advancement.AdvancementEntry
import net.minecraft.item.ItemStack
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import kotlin.jvm.optionals.getOrNull

class AdvancementManager : IAdvancementManager {
    override fun listAdvancements(server: MinecraftServer): List<String> {
        return server.advancementLoader.advancements
            .filter { it.value.display.isPresent }
            .map { it.id.toString() }
    }

    override fun getAdvancement(server: MinecraftServer, id: String): IAdvancementHandle? {
        return server.advancementLoader.get(Identifier.of(id))
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
        for (advancement in player.server.advancementLoader.advancements) {
            val progress = player.advancementTracker.getProgress(advancement)
            for (criteria in progress.obtainedCriteria) {
                player.advancementTracker.revokeCriterion(advancement, criteria)
            }
        }
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