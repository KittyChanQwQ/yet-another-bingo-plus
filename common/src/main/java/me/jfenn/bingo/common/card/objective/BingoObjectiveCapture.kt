package me.jfenn.bingo.common.card.objective

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.utils.PlayerProfileType

@Serializable
data class BingoObjectiveCapture(
    val team: BingoTeamKey,
    val player: PlayerProfileType,
    val instant: Instant,
)