package me.jfenn.bingo.common.stats.data

import kotlin.time.Duration

class PlayerStatsSummary(
    val totalPlaytime: Duration,
    val totalItems: Long,
    val totalGames: Long,
    val totalWins: Long,
    val totalLosses: Long,
    val monthlyPlaytime: Duration,
    val monthlyItems: Long,
    val monthlyGames: Long,
    val monthlyWins: Long,
    val monthlyLosses: Long,
)