package me.jfenn.bingo.common.stats

import kotlinx.datetime.Clock
import me.jfenn.bingo.common.stats.data.GameInfo
import me.jfenn.bingo.common.stats.data.GamePlayerInfo
import me.jfenn.bingo.common.stats.data.GameTeamInfo
import me.jfenn.bingo.common.stats.data.PlayerStatsSummary
import me.jfenn.bingo.stats.sql.StatsDatabase
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class StatsService(
    private val db: StatsDatabase,
) {

    fun insertGame(
        game: GameInfo,
        teams: List<GameTeamInfo>,
        players: List<GamePlayerInfo>
    ) {
        db.transaction {
            db.gameQueries.upsert(game.toGame())
            for (team in teams) {
                db.gameTeamQueries.upsert(team.toGameTeam())
            }
            for (player in players) {
                db.gamePlayerQueries.upsert(player.toGamePlayer())
            }
        }
    }

    fun fetchGamesByPlayer(playerUuid: UUID, itemsPerPage: Long = 10L) = sequence {
        for (offset in 0L until Long.MAX_VALUE step itemsPerPage) {
            val games = db.gameQueries.allGamesByPlayer(
                minecraft_id = playerUuid.toString(),
                limit = itemsPerPage,
                offset = offset
            ).executeAsList()

            if (games.isEmpty()) break

            yieldAll(games)
        }
    }

    fun getPlayerSummary(playerUuid: UUID): PlayerStatsSummary {
        val total = db.gameQueries.totalStatsByPlayer(playerUuid.toString()).executeAsOne()
        val monthly = db.gameQueries.monthlyStatsByPlayer(playerUuid.toString(), Clock.System.now().toString()).executeAsOne()
        return PlayerStatsSummary(
            totalPlaytime = total.duration?.milliseconds ?: Duration.ZERO,
            totalItems = total.items ?: 0L,
            totalGames = total.games,
            totalWins = total.wins,
            totalLosses = total.losses,
            monthlyPlaytime = monthly.duration?.milliseconds ?: Duration.ZERO,
            monthlyItems = monthly.items ?: 0L,
            monthlyGames = monthly.games,
            monthlyWins = monthly.wins,
            monthlyLosses = monthly.losses,
        )
    }

}