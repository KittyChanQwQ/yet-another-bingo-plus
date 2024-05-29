package me.jfenn.bingo.common.stats

import kotlinx.datetime.Clock
import me.jfenn.bingo.common.stats.data.*
import me.jfenn.bingo.stats.sql.StatsDatabase
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class StatsService(
    private val db: StatsDatabase,
) {

    fun checkIfIdsExist(playerUuid: UUID, gameIds: Set<UUID>): Set<UUID> {
        val existingIds = db.gameQueries.checkIfIdsExist(
            id = gameIds.map { it.toString() },
            minecraft_id = playerUuid.toString(),
        ).executeAsList()
            .map { UUID.fromString(it) }
            .toSet()

        return gameIds - existingIds
    }

    fun findById(gameId: UUID, playerId: UUID): PlayerGameSummary? {
        val game = db.gameQueries.findById(gameId.toString())
            .executeAsOneOrNull()
            ?: return null
        val player = db.gamePlayerQueries.findById(minecraft_id = playerId.toString(), game_id = gameId.toString())
            .executeAsOneOrNull()
            ?: return null
        val team = db.gameTeamQueries.findById(id = player.team_id, game_id = gameId.toString())
            .executeAsOneOrNull()
            ?: return null

        return PlayerGameSummary(
            game = GameInfo(game),
            team = GameTeamInfo(team),
            player = GamePlayerInfo(player),
        )
    }

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

    fun fetchGamesByPlayer(
        playerUuid: UUID,
        itemsPerPage: Long = 10L
    ) = sequence {
        for (offset in 0L until Long.MAX_VALUE step itemsPerPage) {
            val games = db.gameQueries.allGamesByPlayer(
                minecraft_id = playerUuid.toString(),
                limit = itemsPerPage,
                offset = offset
            ).executeAsList()

            if (games.isEmpty()) break

            yield(games.map { UUID.fromString(it) })
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