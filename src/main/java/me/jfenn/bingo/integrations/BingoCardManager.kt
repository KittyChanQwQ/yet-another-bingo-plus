package me.jfenn.bingo.integrations

import me.jfenn.bingo.BingoGame
import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.map.MapRenderService
import me.jfenn.bingo.state.GameState

object BingoCardManager {

    fun init() {
        GameState.PLAYING.onEnter { prevState ->
            // set up the BINGO card items / initial map render
            if (prevState == GameState.PREGAME) {
                BingoGame.rollCard(this)
            } else {
                // update all card map images
                for (team in BingoTeam.entries) {
                    MapRenderService.update(this, team)
                }
            }

        }
    }

}