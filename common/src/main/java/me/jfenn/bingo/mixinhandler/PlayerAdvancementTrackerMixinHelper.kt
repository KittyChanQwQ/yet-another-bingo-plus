package me.jfenn.bingo.mixinhandler

import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.platform.scope.BingoKoin
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity

object PlayerAdvancementTrackerMixinHelper {
    fun shouldPreventSpectatorAdvancements(server: MinecraftServer, player: ServerPlayerEntity): Boolean {
        return BingoKoin.getScope(server)
            ?.get<TeamService>()
            ?.isPlaying(player) == false
    }
}