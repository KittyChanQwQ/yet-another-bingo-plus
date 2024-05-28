package me.jfenn.bingo.common.stats

import kotlinx.serialization.encodeToString
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.utils.json
import me.jfenn.bingo.stats.sql.Game
import me.jfenn.bingo.stats.sql.GamePlayer
import me.jfenn.bingo.stats.sql.GameTeam
import me.jfenn.bingo.stats.sql.StatsDatabase
import org.slf4j.Logger
import java.security.MessageDigest
import java.util.*
import kotlin.time.measureTime

class WriteStatsController(
    private val log: Logger,
    private val state: BingoState,
    private val db: StatsDatabase,
    events: ScopedEvents,
) {
    private fun hashSha(string: String): String {
        val md = MessageDigest.getInstance("SHA-512")
        val digest = md.digest(string.toByteArray())
        return HexFormat.of().formatHex(digest)
    }

    private fun writeGameResult() {
        val insertPlayers = mutableListOf<GamePlayer>()
        val insertTeams = mutableListOf<GameTeam>()

        val teams = state.getRegisteredTeams()

        // the game is a draw if nobody won and there is more than one team
        val isDraw = teams.none { it.isWinner() } && teams.size > 1
        val options = json.encodeToString(state.options)
        val insertGame = Game(
            id = state.gameId,
            bingo_options = options,
            bingo_options_hash = hashSha(options),
            started_at = json.encodeToString(state.startedAt),
            ended_at = json.encodeToString(state.endedAt),
            duration = json.encodeToString(state.ingameDuration()),
            player_count = teams.sumOf { it.players.size.toLong() },
            is_draw = if (isDraw) 1L else 0L,
            is_forfeit = if (state.isForfeit) 1L else 0L,
        )

        for (team in teams) {
            val insertTeam = GameTeam(
                id = team.id,
                game_id = insertGame.id,
                name = team.name.key,
                is_winner = if (team.isWinner()) 1L else 0L
            ).also { insertTeams.add(it) }

            for (player in team.players) {
                val itemCount = state.card.entries.count { it.players.contains(player) }
                insertPlayers.add(GamePlayer(
                    team_id = insertTeam.id,
                    game_id = insertGame.id,
                    minecraft_id = player.uuid,
                    minecraft_name = player.name,
                    captured_items = itemCount.toLong(),
                ))
            }
        }

        db.transaction {
            db.gameQueries.upsert(insertGame)
            for (insertTeam in insertTeams) {
                db.gameTeamQueries.upsert(insertTeam)
            }
            for (insertPlayer in insertPlayers) {
                db.gamePlayerQueries.upsert(insertPlayer)
            }
        }
    }

    init {
        events.onEnter(GameState.POSTGAME) {
            log.info("Writing game result to stats.db...")
            measureTime {
                writeGameResult()
            }.also {
                log.info("Done! ($it)")
            }
        }
    }
}