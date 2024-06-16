package me.jfenn.bingo.client.common.hud

import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.map.CardDisplayPacket
import me.jfenn.bingo.common.map.CardView
import me.jfenn.bingo.common.game.GameOverPacket

class BingoHudState(
    var display: CardDisplayPacket? = null,
    val views: MutableMap<BingoTeamKey?, CardView> = mutableMapOf(),
    var selectedTeam: BingoTeamKey? = null,
    var gameOver: GameOver? = null,
) {
    fun reset() {
        display = null
        views.clear()
        selectedTeam = null
        gameOver = null
    }

    class GameOver(
        var packet: GameOverPacket,
        var views: List<CardView>,
    )
}
