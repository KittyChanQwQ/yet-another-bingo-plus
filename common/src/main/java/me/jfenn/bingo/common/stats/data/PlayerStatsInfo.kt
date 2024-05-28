package me.jfenn.bingo.common.stats.data

import kotlin.time.Duration

class PlayerStatsInfo(
    val totalPlaytime: Duration,
    val totalGames: Long,
    val totalWins: Long,
    val totalLosses: Long,
    val monthlyPlaytime: Duration,
    val monthlyGames: Long,
    val monthlyWins: Long,
    val monthlyLosses: Long,
)