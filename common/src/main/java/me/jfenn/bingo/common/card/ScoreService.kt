package me.jfenn.bingo.common.card

import me.jfenn.bingo.common.state.BingoState

object ScoreService {

    fun isLockout(state: BingoState): Boolean {
        if (!state.options.isLockoutMode) return false
        // if we're in inventory mode, then teams can lose tiles...
        // so it shouldn't be possible to get into a stalemate
        if (state.options.isInventoryMode) return false
        // if we're in ranked mode, don't end the game from a lockout at all
        if (state.options.isRankedMode) return false
        // if there's only one team, then the game shouldn't end by lockout...
        if (state.getRegisteredTeams().size <= 1) return false

        val leadingTeam = getLeadingByLines(state, allowTies = false)
        val leadingTeamLines = leadingTeam?.score?.lines
            ?: state.getRegisteredTeams().maxOfOrNull { it.score.lines }
            ?: 0

        // if noone can beat or tie with the leading team, then it is a lockout (the leading team has won)
        val isLockout = state.getRegisteredTeams()
            .filter { it.key != leadingTeam?.key }
            .none { team ->
                val possibleLines = state.card.countLines { entry -> entry.hasAchieved(team) || entry.hasNoneAchieved() }
                // if a team can capture more lines, and it would either tie or beat the current team
                possibleLines > team.score.lines && possibleLines >= leadingTeamLines
            }

        val isGoalImpossible = state.getRegisteredTeams()
            .none { team ->
                val possibleLines = state.card.countLines { entry -> entry.hasAchieved(team) || entry.hasNoneAchieved() }
                // if no team can capture the amount of lines needed for the win condition
                possibleLines >= state.options.goal.numLines
            }

        return isLockout || isGoalImpossible
    }

    fun getLeading(state: BingoState, allowTies: Boolean = false): BingoTeam? {
        return getLeadingByLines(state, allowTies) ?: getLeadingByItems(state, allowTies)
    }

    fun getLeadingByLines(state: BingoState, allowTies: Boolean = false): BingoTeam? {
        var max: BingoTeam? = null
        var maxLines = 0
        for (team in state.getRegisteredTeams()) {
            val teamLines = team.score.lines

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
        for (team in state.getRegisteredTeams()) {
            val teamItems = team.score.items

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