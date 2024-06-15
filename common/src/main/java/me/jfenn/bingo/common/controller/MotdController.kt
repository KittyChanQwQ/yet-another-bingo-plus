package me.jfenn.bingo.common.controller

import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import net.minecraft.server.MinecraftServer

class MotdController(
    events: ScopedEvents,
    private val server: MinecraftServer,
    private val state: BingoState,
) : BingoComponent() {

    private fun updateMotd(state: GameState) {
        server.setMotd("${state.color}[${state.motd}]§f Minecraft BINGO")
    }

    init {
        events.onStateChange { (_, newState) ->
            if (state.isLobbyMode) updateMotd(newState)
        }
    }

}