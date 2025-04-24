package me.jfenn.bingo.common.scoring

import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.team.TeamScore
import kotlin.time.Duration

@Serializable
data class ScoreRanking(
    val index: Int,
    val key: BingoTeamKey,
    val score: TeamScore,
    val duration: Duration?
)