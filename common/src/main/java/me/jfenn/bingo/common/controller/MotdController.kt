package me.jfenn.bingo.common.controller

import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.platform.IMinecraftServer

internal class MotdController(
    events: ScopedEvents,
    private val server: IMinecraftServer,
    private val state: BingoState,
) : BingoComponent() {

    private val GameState.motdPrefix get() = "${color}[$motd]§f "

    private fun updateMotd(state: GameState) {
        var motd = server.messageOfTheDay.takeIf { it.isNotBlank() }
            ?: "Minecraft BINGO"

        for (entry in GameState.entries) {
            motd = motd.removePrefix(entry.motdPrefix)
        }

        server.messageOfTheDay = state.motdPrefix + motd
    }

    init {
        events.onStateChange { (_, newState) ->
            if (state.isLobbyMode) updateMotd(newState)
        }
    }

}