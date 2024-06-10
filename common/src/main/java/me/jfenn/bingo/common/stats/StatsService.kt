package me.jfenn.bingo.common.stats

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jfenn.bingo.api.IExecutors
import me.jfenn.bingo.common.stats.data.*
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.stats.sql.StatsDatabase
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class StatsService(
    private val db: StatsDatabase,
    executors: IExecutors,
) {

    private val executor = executors.io

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
        itemsPerPage: Long = 100L
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

    /**
     * Generate an SHA-256 hash of all game IDs played by the player,
     * for syncing purposes.
     */
    fun fetchGamesByPlayerSha512(playerId: UUID): String {
        val md = MessageDigest.getInstance("SHA-512")
        for (games in fetchGamesByPlayer(playerId)) {
            for (game in games) {
                md.update(game.toString().toByteArray())
            }
        }
        val digest = md.digest()
        return HexFormat.of().formatHex(digest)
    }

    fun getPlayerSummary(playerUuid: UUID, now: Instant = Clock.System.now()): PlayerStatsSummary {
        val total = db.gameQueries.totalStatsByPlayer(playerUuid.toString()).executeAsOne()
        val monthly = db.gameQueries.monthlyStatsByPlayer(playerUuid.toString(), now.toString()).executeAsOne()
        val favoriteTeam = db.gamePlayerQueries.findFavoriteTeam(playerUuid.toString()).executeAsOneOrNull()
        val currentStreak = db.gameQueries.latestWinStreakByPlayer(playerUuid.toString()).executeAsOneOrNull()

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
            favoriteTeam = favoriteTeam?.id?.let { BingoTeamKey(it) },
            favoriteTeamPercentage = favoriteTeam?.let { it.count / it.total.toFloat() } ?: 0f,
            winStreak = currentStreak ?: 0L,
        )
    }

    fun getPlayerSummaryAsync(playerId: UUID): CompletableFuture<PlayerStatsSummary> {
        return CompletableFuture<PlayerStatsSummary>()
            .completeAsync({ getPlayerSummary(playerId) }, executor)
            .orTimeout(10L, TimeUnit.SECONDS)
    }

}