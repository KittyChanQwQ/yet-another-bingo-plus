package me.jfenn.bingo.common.game

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.BingoWinCondition
import me.jfenn.bingo.common.config.EndWhen
import me.jfenn.bingo.common.scoring.ScoreService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.stats.StatsService
import me.jfenn.bingo.common.team.BingoPlayerProfile
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.UuidAsString
import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import kotlin.time.Duration

class GameOverService(
    private val state: BingoState,
    private val options: BingoOptions,
    private val textProvider: TextProvider,
    private val stats: StatsService,
    private val server: MinecraftServer,
) {

    @Serializable
    class GameOverInfo(
        val reason: GameEndReason?,
        val endedAt: Instant,
        val winningTeamKey: BingoTeamKey?,
        val leadingTeamKey: LeadingTeam?,
        val playerInfo: Map<UuidAsString, GameOverPlayerInfo>,
        val isBestTime: Boolean,
        val prevBestTime: Duration?,
        val scoreRankings: List<GameOverScoreRanking>,
    )

    @Serializable
    class GameOverPlayerInfo(
        val winStreak: Long,
        val bestWinStreak: Long?,
        val isBestWinStreak: Boolean,

        val capturedItems: Int,
        val bestCapturedItems: Int?,
        val isBestCapturedItems: Boolean,
    )

    @Serializable
    sealed class LeadingTeam {
        abstract val leader: BingoTeamKey

        @Serializable
        @SerialName("cards")
        class Cards(override val leader: BingoTeamKey): LeadingTeam()
        @Serializable
        @SerialName("lines")
        class Lines(override val leader: BingoTeamKey): LeadingTeam()
        @Serializable
        @SerialName("items")
        class Items(override val leader: BingoTeamKey): LeadingTeam()
    }

    fun getTitle(info: GameOverInfo): Text {
        return info.reason?.format(textProvider)
            ?: textProvider.string(StringKey.GameEndGameOver)
    }

    fun getMessage(info: GameOverInfo): Text {
        return (
            Text.empty().takeIf { options.endGameWhen !is EndWhen.FirstWin }
                ?: info.winningTeamKey?.let { state.teams[it] }?.let { winner ->
                    textProvider.string(StringKey.GameEndCompletedCard, winner.getName(textProvider, playerName = true))
                }
                ?: info.leadingTeamKey?.let { leadingTeamKey ->
                    val leader = state.teams[leadingTeamKey.leader]
                        ?.getName(textProvider, playerName = true)
                        ?: Text.literal("[unknown]")

                    when (leadingTeamKey) {
                        is LeadingTeam.Cards -> textProvider.string(StringKey.GameEndWonWithMoreCards, leader)
                        is LeadingTeam.Lines -> textProvider.string(StringKey.GameEndWonWithMoreLines, leader)
                        is LeadingTeam.Items -> textProvider.string(StringKey.GameEndWonWithMoreItems, leader)
                    }
                }
                ?: textProvider.string(StringKey.GameEndDraw)
        ).formatted(Formatting.YELLOW)
    }

    fun getGameInfo(reason: GameEndReason?, endedAt: Instant): GameOverInfo {
        val winningTeams = state.getRegisteredTeams()
            .filter { it.isWinner() }

        // The winning team, if there is only one winner
        val winningTeam = winningTeams
            .takeIf { it.size == 1 }
            ?.firstOrNull()

        val leadingTeam =
            ScoreService.getLeadingByCards(state)?.let { LeadingTeam.Cards(it.key) }
                ?: ScoreService.getLeadingByLines(state)?.let { LeadingTeam.Lines(it.key) }
                ?: ScoreService.getLeadingByItems(state)?.let { LeadingTeam.Items(it.key) }

        val playerInfo = buildMap {
            for (team in state.getRegisteredTeams()) {
                for (player in team.players) {
                    this[player.uuid] = getPlayerInfo(team, player)
                }
            }
        }

        val gameDuration = state.ingameDuration()
        val prevBestTime = stats.getBestTime(options, server.isSingleplayer && state.isSingleplayer())
        // if prevBestTime didn't exist, or if the new duration is less than the prev time
        // ... and the game was actually completed & won by a team
        val isBestTime = (prevBestTime == null || gameDuration?.let { it < prevBestTime } == true)
                && !state.isForfeit
                && winningTeams.isNotEmpty()

        val scoreRankingScores = state.getRegisteredTeams()
            .map { team ->
                val duration = team.winner?.instant?.let { completedAt ->
                    completedAt - (state.startedAt ?: completedAt)
                }

                GameOverScoreRanking(
                    index = 0,
                    key = team.key,
                    score = team.score,
                    duration = duration,
                )
            }
            .sortedWith(
                Comparator.comparingLong<GameOverScoreRanking> { it.duration?.inWholeMilliseconds ?: Long.MAX_VALUE }
                    .thenComparingInt { -it.score.cards }
                    .thenComparingInt { -it.score.lines }
                    .thenComparingInt { -it.score.items }
            )

        val scoreRankings = buildList<GameOverScoreRanking> {
            var index = -1
            for (ranking in scoreRankingScores) {
                // Only increment the index if the teams are not tied
                val prevRanking = lastOrNull()
                if (prevRanking?.score != ranking.score || prevRanking.duration != ranking.duration) {
                    index++
                }

                add(ranking.copy(index = index))
            }
        }
            .let { rankings ->
                // If we're in infinite mode, then there isn't a "finish"
                // - use the game duration as each team's completion time, instead of showing DNF
                if (options.winCondition is BingoWinCondition.Infinite) {
                    rankings.map { it.copy(duration = gameDuration) }
                } else {
                    rankings
                }
            }

        return GameOverInfo(
            reason = reason,
            winningTeamKey = winningTeam?.key,
            leadingTeamKey = leadingTeam,
            playerInfo = playerInfo,
            endedAt = endedAt,
            isBestTime = isBestTime,
            prevBestTime = prevBestTime,
            scoreRankings = scoreRankings,
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

        val capturedItems = team.completedCards.sumOf { completion ->
            completion.card.countItems { it.players.containsKey(player.uuid) }
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