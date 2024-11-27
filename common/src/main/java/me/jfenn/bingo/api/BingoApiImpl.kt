package me.jfenn.bingo.api

import me.jfenn.bingo.api.data.*
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.stats.StatsService
import java.util.*
import kotlin.time.toJavaDuration

internal class BingoApiImpl(
    private val state: BingoState,
    private val statsService: StatsService,
) : IBingoApi {
    override val game: BingoGame
        get() = BingoGame(
            id = state.gameId,
            status = when (state.state) {
                GameState.UNINITIALIZED,
                    GameState.PREGAME,
                    GameState.STARTING,
                    GameState.PRELOADING,
                    GameState.LOADING,
                    GameState.COUNTDOWN -> BingoGameStatus.PREGAME
                GameState.PLAYING -> BingoGameStatus.PLAYING
                GameState.POSTGAME -> BingoGameStatus.POSTGAME
            },
            time = state.ingameDuration()?.toJavaDuration(),
            timeRemaining = state.remainingDuration()?.toJavaDuration()
        )

    override val teams: IBingoTeams = TeamsImpl()

    override fun getPlayerStats(id: UUID): IBingoPlayerStats {
        val stats = statsService.getPlayerSummary(id)
        return BingoPlayerStatsImpl(stats)
    }

    inner class TeamsImpl: IBingoTeams {
        override fun iterator(): Iterator<IBingoTeam> {
            return state.getRegisteredTeams()
                .map { BingoTeamImpl(it) }
                .iterator()
        }
    }

}