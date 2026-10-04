package me.jfenn.bingo.integrations.chunky

import me.jfenn.bingo.api.state.GameState
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.game.GameResetEvent

internal class ChunkyController(
    events: ScopedEvents,
    eventBus: IEventBus,
    private val chunkyIntegration: IChunkyApi,
) : BingoComponent() {

    init {
        events.onEnter(GameState.PREGAME) {
            chunkyIntegration.startPregen()
        }

        events.onEnter(GameState.STARTING) {
            chunkyIntegration.cancelTasks()
        }

        eventBus.register(GameResetEvent.BeforeClose) {
            chunkyIntegration.clearAllTasks()
        }
    }

}
