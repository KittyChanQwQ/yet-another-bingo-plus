package me.jfenn.bingo.card

import me.jfenn.bingo.config.BingoConfig
import me.jfenn.bingo.config.BingoMode
import me.jfenn.bingo.state.BingoState

object ScoreService {

    fun getWinner(state: BingoState): BingoTeam? {
        for (team in BingoTeam.entries) {
            val score = state.getTeamScore(team)
            if (score.lines >= state.config.winCondition.numLines) return team
        }

        return null
    }

    fun isStalemate(state: BingoState): Boolean {
        if (state.config.gameMode != BingoMode.LOCKOUT) return false

        // check all teams for the number of capture-able lines
        // if any team can capture a line, it is not a stalemate
        for (team in BingoTeam.entries) {
            // count number of lines that the team can achieve
            val possibleLines = state.card.countLines { entry -> entry.hasAchieved(team) || entry.hasNoneAchieved() }
            if (possibleLines >= 1) return false
        }

        return true
    }

    fun getLeading(state: BingoState, allowTies: Boolean = false): BingoTeam? {
        return getLeadingByLines(state, allowTies) ?: getLeadingByItems(state, allowTies)
    }

    fun getLeadingByLines(state: BingoState, allowTies: Boolean = false): BingoTeam? {
        var max: BingoTeam? = null
        var maxLines = 0
        for (team in BingoTeam.entries) {
            val teamLines = state.getTeamScore(team).lines

            if (teamLines > maxLines) {
                max = team
                maxLines = teamLines
            } else if (teamLines == maxLines && !allowTies) {
                max = null
            }
        }

        return max
    }

    fun getLeadingByItems(state: BingoState, allowTies: Boolean = false): BingoTeam? {
        var max: BingoTeam? = null
        var maxItems = 0
        for (team in BingoTeam.entries) {
            val teamItems = state.getTeamScore(team).items

            if (teamItems > maxItems) {
                max = team
                maxItems = teamItems
            } else if (teamItems == maxItems && !allowTies) {
                max = null
            }
        }

        return max
    }

}