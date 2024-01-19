package me.jfenn.bingo.common.integrations

import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.state.BingoContext
import me.jfenn.bingo.common.state.GameState
import net.minecraft.server.MinecraftServer

class MotdController(
    events: ScopedEvents,
    val server: MinecraftServer,
) {

    fun updateMotd(state: GameState) {
        server.setMotd("${state.color}[${state.motd}]§f Minecraft BINGO")
    }

    init {
        events.onStateChange { (oldState, newState) ->
            updateMotd(newState)
        }
    }

}