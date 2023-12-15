package me.jfenn.bingo.integrations

import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.card.CardService
import me.jfenn.bingo.map.MapRenderService
import me.jfenn.bingo.state.BingoContext
import me.jfenn.bingo.state.GameState

/**
 * Handles re-rolling or generating new BINGO card layouts.
 */
object BingoCardController {

    fun rollCard(ctx: BingoContext) {
        ctx.state.card = CardService.generate(ctx.config)

        // update the card map images
        for (team in BingoTeam.entries) {
            MapRenderService.update(ctx, team)
        }
    }

    fun init() {
        GameState.PLAYING.onEnter { prevState ->
            // set up the BINGO card items / initial map render
            if (prevState == GameState.PREGAME) {
                rollCard(this)
            } else {
                // update all card map images
                for (team in BingoTeam.entries) {
                    MapRenderService.update(this, team)
                }
            }

        }
    }

}