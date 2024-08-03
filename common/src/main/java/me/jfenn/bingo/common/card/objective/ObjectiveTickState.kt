package me.jfenn.bingo.common.card.objective

import me.jfenn.bingo.common.team.BingoTeamKey
import java.util.*

class ObjectiveTickState(
    val activeTeams: Set<BingoTeamKey>,
    val activePlayers: Set<UUID>,
)