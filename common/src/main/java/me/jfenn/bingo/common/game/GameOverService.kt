package me.jfenn.bingo.common.game

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.config.BingoGoal
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.scoring.ScoreService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.stats.StatsService
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.TeamService
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
    private val config: BingoConfig,
    private val textProvider: TextProvider,
    private val teamService: TeamService,
    private val stats: StatsService,
    private val server: MinecraftServer,
) {

    class GameOverInfo(
        val title: Text,
        val message: Text,
        val endedAt: Instant,
        val winningTeam: BingoTeam?,
        val winStreaksByPlayer: Map<UUID, Int>,
        val isBestTime: Boolean,
        val prevBestTime: Duration?,
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
                        textProvider.string(StringKey.GameEndCompletedCard, teamService.getTeamOrPlayerName(winner))
                    }
                    ?: ScoreService.getLeadingByLines(state)
                        ?.takeIf { state.options.cardGoal is BingoGoal.Lines }
                        ?.let { leader ->
                            textProvider.string(StringKey.GameEndWonWithMoreLines, teamService.getTeamOrPlayerName(leader))
                        }
                    ?: ScoreService.getLeadingByItems(state)?.let { leader ->
                        textProvider.string(StringKey.GameEndWonWithMoreItems, teamService.getTeamOrPlayerName(leader))
                    }
                    ?: textProvider.string(StringKey.GameEndDraw)
                ).formatted(Formatting.YELLOW)

        val winStreaksByPlayer = state.getRegisteredTeams()
            .flatMap { it.players }
            .associate { player ->
                player.uuid to stats.getPlayerWinStreak(player.uuid)
            }

        val prevBestTime = stats.getBestTime(options, config.statsHostId, server.isSingleplayer && server.currentPlayerCount == 1)
        // if prevBestTime didn't exist, or if the new duration is less than the prev time
        // ... and the game was actually completed & won by a team
        val isBestTime = (prevBestTime == null || state.ingameDuration()?.let { it < prevBestTime } == true)
                && !state.isForfeit
                && state.getRegisteredTeams().any { it.isWinner() }

        return GameOverInfo(
            title = title,
            message = message,
            winningTeam = winningTeam,
            winStreaksByPlayer = winStreaksByPlayer,
            endedAt = Clock.System.now(),
            isBestTime = isBestTime,
            prevBestTime = prevBestTime,
        )
    }
}