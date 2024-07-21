package me.jfenn.bingo.client.common.hud

import kotlinx.datetime.Instant
import me.jfenn.bingo.common.game.GameOverPacket
import me.jfenn.bingo.common.map.CardDisplayPacket
import me.jfenn.bingo.common.map.CardView
import me.jfenn.bingo.common.scoring.ScoreMessagePacket
import me.jfenn.bingo.common.team.BingoTeamKey

class BingoHudState(
    var display: CardDisplayPacket? = null,
    val views: MutableMap<BingoTeamKey?, CardView> = mutableMapOf(),
    var selectedTeam: BingoTeamKey? = null,
    var gameOver: GameOver? = null,
    val messages: MutableList<ScoreMessage> = mutableListOf(),
) {
    fun reset() {
        display = null
        views.clear()
        selectedTeam = null
        gameOver = null
        messages.clear()
    }

    class GameOver(
        var packet: GameOverPacket,
        var views: List<CardView>,
    )

    class ScoreMessage(
        val createdAt: Instant,
        val packet: ScoreMessagePacket,
    )
}
