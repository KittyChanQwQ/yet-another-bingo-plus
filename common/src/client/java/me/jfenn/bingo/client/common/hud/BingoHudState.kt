package me.jfenn.bingo.client.common.hud

import me.jfenn.bingo.common.card.BingoTeamKey
import me.jfenn.bingo.common.map.CardTeams
import me.jfenn.bingo.common.map.CardView
import me.jfenn.bingo.common.text.GameOverPacket

class BingoHudState(
    var display: CardTeams? = null,
    val views: MutableMap<BingoTeamKey?, CardView> = mutableMapOf(),
    var selectedTeam: BingoTeamKey? = null,
    var gameOverPacket: GameOverPacket? = null,
)
