package me.jfenn.bingo.mixinhandler

import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.platform.scope.BingoKoin
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity

object PlayerAdvancementTrackerMixinHelper {
    fun shouldPreventSpectatorAdvancements(server: MinecraftServer, player: ServerPlayerEntity): Boolean {
        val scope = BingoKoin.getScope(server) ?: return false
        return scope.get<BingoState>().isLobbyMode &&
                !scope.get<TeamService>().isPlaying(player)
    }
}