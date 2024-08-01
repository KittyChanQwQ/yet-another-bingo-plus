package me.jfenn.bingo.common.card.objective

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.team.BingoPlayerProfile
import me.jfenn.bingo.common.team.BingoTeamKey

@Serializable
data class BingoObjectiveCapture(
    val team: BingoTeamKey,
    val player: BingoPlayerProfile,
    val instant: Instant,
)