package me.jfenn.bingo.common.controller

import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.map.MapRenderService
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import org.slf4j.Logger

/**
 * Handles re-rolling or generating new BINGO card layouts.
 */
class BingoCardController(
    events: ScopedEvents,
    private val state: BingoState,
    private val mapRenderService: MapRenderService,
    private val cardService: CardService,
) : BingoComponent() {
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

        events.onEnter(GameState.PREGAME) {
            cardService.generateCard()
        }

        events.onEnter(GameState.POSTGAME) {
            // Mark all items as "seen" when game ends
            // (if hidden items is true, players should see all items after the game)
            for (team in state.teams.values) {
                for (item in state.card.itemIndex.values) {
                    item.markSeen(team)
                }
            }
        }
    }
}