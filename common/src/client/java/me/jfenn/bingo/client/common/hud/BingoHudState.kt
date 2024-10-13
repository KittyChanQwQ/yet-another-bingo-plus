package me.jfenn.bingo.client.common.hud

import kotlinx.datetime.Instant
import me.jfenn.bingo.client.api.INativeImage
import me.jfenn.bingo.common.game.GameOverPacket
import me.jfenn.bingo.common.map.CardDisplayPacket
import me.jfenn.bingo.common.map.CardView
import me.jfenn.bingo.common.menu.tooltips.TooltipPacket
import me.jfenn.bingo.common.scoring.ScoreMessagePacket
import me.jfenn.bingo.common.team.BingoTeamKey
import java.util.concurrent.ConcurrentHashMap

class BingoHudState(
    var display: CardDisplayPacket? = null,
    val views: MutableMap<BingoTeamKey?, CardView> = mutableMapOf(),
    var selectedTeam: BingoTeamKey? = null,
    var gameOver: GameOver? = null,
    val messages: MutableList<ScoreMessage> = mutableListOf(),
    val images: MutableMap<String, INativeImage> = ConcurrentHashMap(),
    var tooltip: TooltipPacket? = null,
    var tooltipStartedAt: Instant? = null,
) {
    fun reset() {
        display = null
        selectedTeam = null
        gameOver = null
        messages.clear()
        tooltip = null
        tooltipStartedAt = null
    }

    fun resetAll() {
        reset()
        views.clear()

        val clearedImages = images.values.toList()
        images.clear()
        for (clearedImage in clearedImages) {
            clearedImage.close()
        }
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
