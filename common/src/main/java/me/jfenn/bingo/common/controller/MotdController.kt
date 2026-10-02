package me.jfenn.bingo.common.controller

import me.jfenn.bingo.api.data.BingoGameStatus
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.platform.IMinecraftServer

internal class MotdController(
    events: ScopedEvents,
    private val server: IMinecraftServer,
    private val state: BingoState,
) : BingoComponent() {

    private val BingoGameStatus.color get() = when (this) {
        BingoGameStatus.PREGAME -> "§a"
        BingoGameStatus.STARTING,
        BingoGameStatus.PLAYING -> "§c"
        BingoGameStatus.POSTGAME -> "§6"
    }
    private val BingoGameStatus.motd get() = when (this) {
        BingoGameStatus.PREGAME -> "PRE-GAME"
        BingoGameStatus.STARTING,
        BingoGameStatus.PLAYING -> "IN-GAME"
        BingoGameStatus.POSTGAME -> "POST-GAME"
    }
    private val BingoGameStatus.motdPrefix get() = "${color}[$motd]§f "

    private fun updateMotd(state: BingoGameStatus) {
        var motd = server.messageOfTheDay.takeIf { it.isNotBlank() }
            ?: "Minecraft BINGO"

        for (entry in BingoGameStatus.entries) {
            motd = motd.removePrefix(entry.motdPrefix)
        }

        server.messageOfTheDay = state.motdPrefix + motd
    }

    init {
        events.onStateChange { (_, newState) ->
            if (state.isLobbyMode) updateMotd(newState.status)
        }
    }

}