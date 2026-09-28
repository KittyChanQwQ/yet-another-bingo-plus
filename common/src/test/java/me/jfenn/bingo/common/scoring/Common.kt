package me.jfenn.bingo.common.scoring

import io.mockk.every
import io.mockk.mockk
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.team.TeamState
import me.jfenn.bingo.common.team.TeamScore
import java.time.Duration
import java.time.Instant

internal fun mockTeam(
    name: String,
    score: TeamScore,
    state: TeamState = TeamState.Playing(mockk()),
) = mockk<BingoTeam>().also {
    every { it.id } returns name
    every { it.key } returns BingoTeamKey(name)
    every { it.countCards() } returns score.cards
    every { it.score } returns score
    every { it.state } returns state
}

internal fun mockState(
    teams: List<BingoTeam>,
    startedAt: Instant = Instant.ofEpochSecond(1746464705),
) = mockk<BingoState>().also {
    every { it.getRegisteredTeams() } returns teams
    every { it.cards } returns mutableListOf()
    every { it.startedAt } returns startedAt
    every { it.timeAdjustment } returns Duration.ZERO
    every { it.timeOffline } returns Duration.ZERO
}
