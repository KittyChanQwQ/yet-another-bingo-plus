package me.jfenn.bingo.common.game

import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.scoring.ScoreService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class GameOverService(
    private val state: BingoState,
    private val options: BingoOptions,
    private val textProvider: TextProvider,
    private val teamService: TeamService,
) {

    class GameOverInfo(
        val title: Text,
        val message: Text,
        val winningTeam: BingoTeam?,
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
                    ?: ScoreService.getLeadingByLines(state)?.let { leader ->
                        textProvider.string(StringKey.GameEndWonWithMoreLines, teamService.getTeamOrPlayerName(leader))
                    }
                    ?: ScoreService.getLeadingByItems(state)?.let { leader ->
                        textProvider.string(StringKey.GameEndWonWithMoreItems, teamService.getTeamOrPlayerName(leader))
                    }
                    ?: textProvider.string(StringKey.GameEndDraw)
                ).formatted(Formatting.YELLOW)

        return GameOverInfo(
            title = title,
            message = message,
            winningTeam = winningTeam,
        )
    }
}