package me.jfenn.bingo.common.integrations

import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.map.MapRenderService
import me.jfenn.bingo.common.state.BingoContext
import me.jfenn.bingo.common.state.GameState

/**
 * Handles re-rolling or generating new BINGO card layouts.
 */
object BingoCardController {

    fun rollCard(ctx: BingoContext) {
        if (!ctx.config.isValid()) return

        ctx.state.card = CardService.generate(ctx.config, ctx.state.seed)

        if (ctx.state.state == GameState.PREGAME) {
            // if in pregame, update the preview card!
            MapRenderService.update(ctx, team = null, teamMapId = ctx.state.getPreviewMapId(ctx))
        }

        if (ctx.state.state == GameState.PLAYING) {
            // update the card map images
            for (team in BingoTeam.entries) {
                MapRenderService.update(ctx, team)
            }
        }
    }

    fun init() {
        GameState.PREGAME.onEnter {
            // if in pregame, update the preview card!
            MapRenderService.update(this, team = null, teamMapId = state.getPreviewMapId(this))
        }

        GameState.PLAYING.onEnter {
            // update all card map images
            for (team in BingoTeam.entries) {
                MapRenderService.update(this, team)
            }
        }
    }

}