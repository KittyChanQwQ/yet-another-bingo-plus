package me.jfenn.bingo.impl

import me.jfenn.bingo.api.IAdvancementManager
import net.minecraft.server.network.ServerPlayerEntity

class AdvancementManager : IAdvancementManager {

    override fun clearAdvancements(player: ServerPlayerEntity) {
        for (advancement in player.server.advancementLoader.advancements) {
            val progress = player.advancementTracker.getProgress(advancement)
            for (criteria in progress.obtainedCriteria) {
                player.advancementTracker.revokeCriterion(advancement, criteria)
            }
        }
    }

}