package me.jfenn.bingo.integrations.chunky

import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.GameState
import net.minecraft.server.MinecraftServer

class ChunkyController(
    events: ScopedEvents,
    private val server: MinecraftServer,
    private val chunkyIntegration: ChunkyIntegration,
) : BingoComponent() {

    init {
        events.onEnter(GameState.PREGAME) {
            chunkyIntegration.startPregen(server)
        }

        events.onEnter(GameState.STARTING) {
            chunkyIntegration.cancelTasks(server)
        }

        events.onEnter(GameState.PRELOADING) {
            chunkyIntegration.cancelTasks(server)
        }

        events.onEnter(GameState.PLAYING) {
            chunkyIntegration.cancelTasks(server)
        }
    }

}