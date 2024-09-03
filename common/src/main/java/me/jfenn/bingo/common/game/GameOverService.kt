package me.jfenn.bingo.common.game

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jfenn.bingo.common.config.BingoGoal
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.scoring.ScoreService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.stats.StatsService
import me.jfenn.bingo.common.team.BingoPlayerProfile
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.*
import kotlin.time.Duration

class GameOverService(
    private val state: BingoState,
    private val options: BingoOptions,
    private val textProvider: TextProvider,
    private val stats: StatsService,
    private val server: MinecraftServer,
) {

    class GameOverInfo(
        val title: Text,
        val message: Text,
        val endedAt: Instant,
        val winningTeam: BingoTeam?,
        val playerInfo: Map<UUID, GameOverPlayerInfo>,
        val isBestTime: Boolean,
        val prevBestTime: Duration?,
    )

    class GameOverPlayerInfo(
        val winStreak: Long,
        val bestWinStreak: Long?,
        val isBestWinStreak: Boolean,

        val capturedItems: Int,
        val bestCapturedItems: Int?,
        val isBestCapturedItems: Boolean,
    )

    fun getGameInfo(): GameOverInfo {
        val winningTeams = state.getRegisteredTeams()
            .filter { it.isWinner() }

        // The winning team, if there is only one winner
        val winningTeam = winningTeams
            .takeIf { it.size == 1 }
            ?.firstOrNull()

        val title = state.endReason
            ?.let { textProvider.string(it.stringKey) }
            ?: textProvider.string(StringKey.GameEndGameOver)

        val message = (
                Text.empty()
                    .takeIf { options.isRankedMode }
                    ?: winningTeam?.let { winner ->
                        textProvider.string(StringKey.GameEndCompletedCard, winner.getName(textProvider, playerName = true))
                    }
                    // TODO: needs to account for # of completed cards (+ ScoreService)
                    ?: ScoreService.getLeadingByLines(state)
                        ?.takeIf { state.cards.last().options.goal is BingoGoal.Lines }
                        ?.let { leader ->
                            textProvider.string(StringKey.GameEndWonWithMoreLines, leader.getName(textProvider, playerName = true))
                        }
                    ?: ScoreService.getLeadingByItems(state)?.let { leader ->
                        textProvider.string(StringKey.GameEndWonWithMoreItems, leader.getName(textProvider, playerName = true))
                    }
                    ?: textProvider.string(StringKey.GameEndDraw)
                ).formatted(Formatting.YELLOW)

        val playerInfo = buildMap {
            for (team in state.getRegisteredTeams()) {
                for (player in team.players) {
                    this[player.uuid] = getPlayerInfo(team, player)
                }
            }
        }

        val prevBestTime = stats.getBestTime(options, server.isSingleplayer && state.isSingleplayer())
        // if prevBestTime didn't exist, or if the new duration is less than the prev time
        // ... and the game was actually completed & won by a team
        val isBestTime = (prevBestTime == null || state.ingameDuration()?.let { it < prevBestTime } == true)
                && !state.isForfeit
                && winningTeams.isNotEmpty()

        return GameOverInfo(
            title = title,
            message = message,
            winningTeam = winningTeam,
            playerInfo = playerInfo,
            endedAt = Clock.System.now(),
            isBestTime = isBestTime,
            prevBestTime = prevBestTime,
        )
    }

    private fun getPlayerInfo(team: BingoTeam, player: BingoPlayerProfile): GameOverPlayerInfo {
        val isWinner = team.isWinner()

        // must align with the logic in WriteStatsService
        // - a game is a draw if there is at least one opponent and no winning team
        val teams = state.getRegisteredTeams()
        val isDraw = teams.none { it.isWinner() } && teams.size > 1

        val winStreak = when {
            isWinner -> stats.getPlayerWinStreak(player.uuid) + 1
            // if the game is a forfeit or draw, the current win streak persists
            state.isForfeit || isDraw -> stats.getPlayerWinStreak(player.uuid)
            // otherwise, the game was lost
            else -> 0
        }
        val bestWinStreak = stats.getBestWinStreak(player.uuid)

        val capturedItems = state.cards.sumOf { card ->
            card.countItems { it.players.containsKey(player.uuid) }
        }
        val bestCapturedItems = stats.getBestCapturedItems(player.uuid, options)

        return GameOverPlayerInfo(
            winStreak = winStreak,
            bestWinStreak = bestWinStreak,
            isBestWinStreak = isWinner && bestWinStreak?.let { winStreak > it } == true,

            capturedItems = capturedItems,
            bestCapturedItems = bestCapturedItems,
            isBestCapturedItems = bestCapturedItems?.let { capturedItems > it } == true,
        )
    }
}