package me.jfenn.bingo.common.controller

import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.integrations.ChunkyIntegration
import net.minecraft.server.MinecraftServer

class IntegrationController(
    events: ScopedEvents,
    private val server: MinecraftServer,
    private val chunkyIntegration: ChunkyIntegration,
) : BingoComponent() {

    init {
        events.onEnter(GameState.PREGAME) {
            chunkyIntegration.startPregen(server)
        }

        events.onEnter(GameState.PLAYING) {
            chunkyIntegration.cancelTasks(server)
        }
    }

}