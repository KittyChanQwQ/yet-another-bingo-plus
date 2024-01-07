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
            val previewMap = ctx.state.getPreviewMap(ctx)
            MapRenderService.update(ctx, previewMap)
        }

        if (ctx.state.state == GameState.PLAYING) {
            // update the card map images
            for (team in BingoTeam.entries) {
                val teamMap = ctx.state.getMap(ctx, team)
                MapRenderService.update(ctx, teamMap)
            }
        }
    }

    fun init() {
        GameState.PREGAME.onEnter {
            // if in pregame, update the preview card!
            val previewMap = state.getPreviewMap(ctx)
            MapRenderService.update(ctx, previewMap)
        }

        GameState.PLAYING.onEnter {
            // update all card map images
            for (team in BingoTeam.entries) {
                val teamMap = state.getMap(ctx, team)
                MapRenderService.update(ctx, teamMap)
            }
        }
    }

}