package me.jfenn.bingo.client.common.hud

import me.jfenn.bingo.common.card.BingoTeamKey
import me.jfenn.bingo.common.map.CardTeams
import me.jfenn.bingo.common.map.CardView

class BingoHudState(
    var display: CardTeams? = null,
    val views: MutableMap<BingoTeamKey?, CardView> = mutableMapOf(),
    var selectedTeam: BingoTeamKey? = null,
)
