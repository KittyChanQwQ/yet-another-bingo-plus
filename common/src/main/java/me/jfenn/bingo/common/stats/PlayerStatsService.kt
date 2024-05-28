package me.jfenn.bingo.common.stats

import kotlinx.datetime.Clock
import me.jfenn.bingo.common.stats.data.PlayerStatsInfo
import me.jfenn.bingo.common.team.BingoPlayerProfile
import me.jfenn.bingo.stats.sql.StatsDatabase
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class PlayerStatsService(
    private val db: StatsDatabase,
) {

    fun getStatsForPlayer(player: BingoPlayerProfile): PlayerStatsInfo {
        val total = db.gameQueries.totalStatsByPlayer(player.uuid).executeAsOne()
        val monthly = db.gameQueries.monthlyStatsByPlayer(player.uuid, Clock.System.now().toString()).executeAsOne()
        return PlayerStatsInfo(
            totalPlaytime = total.duration?.milliseconds ?: Duration.ZERO,
            totalGames = total.games,
            totalWins = total.wins,
            totalLosses = total.losses,
            monthlyPlaytime = monthly.duration?.milliseconds ?: Duration.ZERO,
            monthlyGames = monthly.games,
            monthlyWins = monthly.wins,
            monthlyLosses = monthly.losses,
        )
    }

}