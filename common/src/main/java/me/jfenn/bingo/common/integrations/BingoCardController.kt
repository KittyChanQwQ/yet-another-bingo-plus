package me.jfenn.bingo.common.integrations

import me.jfenn.bingo.common.Bingo
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
    }

    fun init() {
        Bingo.onGameTick {
            // update all card map images
            if (state.state == GameState.PREGAME) {
                // if in pregame, update the preview card!
                val previewMap = state.getPreviewMap(ctx)
                MapRenderService.update(ctx, previewMap)
            } else {
                for (team in BingoTeam.entries) {
                    val teamMap = state.getMap(ctx, team)
                    MapRenderService.update(ctx, teamMap)
                }
            }
        }
    }

}