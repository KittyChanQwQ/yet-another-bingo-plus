package me.jfenn.bingo.common.scoring

import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.BingoTeam
import kotlin.time.Duration

internal object ScoreRankingService {

    private fun getTeamDuration(state: BingoState, team: BingoTeam): Duration? {
        return team.winner?.instant?.let { completedAt ->
            completedAt - (state.startedAt ?: completedAt)
        }
            ?.plus(state.timeAdjustment)
            ?.minus(state.timeOffline)
    }

    fun getScoreRankings(state: BingoState): List<ScoreRanking> {
        val scoreRankingComparator = Comparator.comparingLong<BingoTeam> { team ->
            getTeamDuration(state, team)?.inWholeMilliseconds ?: Long.MAX_VALUE
        }
            .thenByDescending(ScoreService.getComparator(state)) { it }

        val scoreRankingScores = state.getRegisteredTeams()
            .sortedWith(scoreRankingComparator)
            .map { team ->
                val duration = getTeamDuration(state, team)

                ScoreRanking(
                    index = 0,
                    key = team.key,
                    score = team.score,
                    duration = duration,
                )
            }

        val scoreRankings = buildList<ScoreRanking> {
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

        return scoreRankings
    }

}