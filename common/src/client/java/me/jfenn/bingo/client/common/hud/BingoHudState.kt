package me.jfenn.bingo.client.common.hud

import kotlinx.datetime.Instant
import me.jfenn.bingo.client.platform.INativeImage
import me.jfenn.bingo.common.game.GameOverPacket
import me.jfenn.bingo.common.map.CardView
import me.jfenn.bingo.common.menu.tooltips.TooltipPacket
import me.jfenn.bingo.common.ready.ReadyUpdatePacket
import me.jfenn.bingo.common.scoring.ScoreMessagePacket
import me.jfenn.bingo.common.team.BingoTeamKey
import java.util.concurrent.ConcurrentHashMap

internal class BingoHudState(
    var now: Instant = Instant.DISTANT_PAST,
    var views: Map<BingoTeamKey?, CardView> = emptyMap(),
    var selectedTeam: BingoTeamKey? = null,
    var gameOver: GameOver? = null,
    val messages: MutableList<ScoreMessage> = mutableListOf(),
    val images: MutableMap<String, INativeImage> = ConcurrentHashMap(),
    var tooltip: TooltipPacket? = null,
    var tooltipStartedAt: Instant? = null,
    var ready: ReadyUpdatePacket? = null,
) {
    fun reset() {
        selectedTeam = null
        gameOver = null
        messages.clear()
        tooltip = null
        tooltipStartedAt = null
        ready = null
    }

    fun resetAll() {
        reset()
        views = emptyMap()

        val clearedImages = images.values.toList()
        images.clear()
        for (clearedImage in clearedImages) {
            clearedImage.close()
        }
    }

    class GameOver(
        var packet: GameOverPacket,
        var views: List<CardView>,
        var tab: GameOverPacket.EndScreenTab = packet.defaultTab,
        var isFirstOpen: Boolean = true,
    )

    class ScoreMessage(
        val createdAt: Instant,
        val packet: ScoreMessagePacket,
    )
}
