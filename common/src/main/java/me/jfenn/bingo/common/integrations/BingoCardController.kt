package me.jfenn.bingo.common.integrations

import me.jfenn.bingo.common.Bingo
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.map.MapRenderService
import me.jfenn.bingo.common.state.BingoContext
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import kotlin.random.Random

/**
 * Handles re-rolling or generating new BINGO card layouts.
 */
class BingoCardController(
    events: ScopedEvents,
    private val state: BingoState,
    private val mapRenderService: MapRenderService,
) {
    init {
        events.onGameTick {
            // update all card map images
            if (state.state == GameState.PREGAME) {
                // if in pregame, update the preview card!
                mapRenderService.update(null)
            } else {
                for (team in state.getRegisteredTeams()) {
                    mapRenderService.update(team)
                }
            }
        }

        events.onEnter(GameState.POSTGAME) {
            // Mark all items as "seen" when game ends
            // (if hidden items is true, players should see all items after the game)
            for (team in state.getRegisteredTeams()) {
                for (item in state.card.itemIndex.values) {
                    item.markSeen(team)
                }
            }
        }
    }
}