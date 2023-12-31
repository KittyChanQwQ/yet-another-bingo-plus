package me.jfenn.bingo.impl

import me.jfenn.bingo.api.IAdvancementManager
import net.minecraft.server.command.CommandOutput
import net.minecraft.server.network.ServerPlayerEntity

class AdvancementManager : IAdvancementManager {

    override fun clearAdvancements(player: ServerPlayerEntity) {
        // The yarn mappings for these APIs conflict between versions, so we're using the command for this instead
        player.server.commandManager.executeWithPrefix(player.server.commandSource.withOutput(CommandOutput.DUMMY), "advancement revoke ${player.entityName} everything")
    }

}