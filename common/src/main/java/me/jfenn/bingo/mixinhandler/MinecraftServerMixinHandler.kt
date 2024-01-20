package me.jfenn.bingo.mixinhandler

import me.jfenn.bingo.common.scope.BingoKoin
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import net.minecraft.server.MinecraftServer

object MinecraftServerMixinHandler {

    fun isGamePlaying(server: MinecraftServer): Boolean {
        val scope = BingoKoin.getScope(server) ?: return false
        val state = scope.get<BingoState>()
        return state.state == GameState.PLAYING
    }

}