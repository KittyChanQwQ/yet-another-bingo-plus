package me.jfenn.bingo.common.scoring

import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.config.BingoGoal
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.BingoTeam

object ScoreService {

    fun isLockout(state: BingoState, card: BingoCard): Boolean {
        if (!card.options.isLockoutMode) return false
        // if we're in inventory mode, then teams can lose tiles...
        // so it shouldn't be possible to get into a stalemate
        if (card.options.isInventoryMode) return false
        // if we're in ranked mode, don't end the game from a lockout at all
        if (state.options.isRankedMode) return false
        // if there's only one team, then the game shouldn't end by lockout...
        if (state.getRegisteredTeams().size <= 1) return false

        if (
            card.entries
                .mapNotNull { card.objectives[it.objectiveId] }
                .all { it.hasAnyAchieved() }
        ) {
            // if the entire card is full, then it is a lockout (regardless of the goal)
            return true
        }

        // if the goal is "full card", disregard any stalemate conditions
        // (otherwise the game would end instantly)
        val cardGoal = card.options.goal
        if (cardGoal.isFullCard())
            return false

        val leadingTeam = getLeadingByLines(state, allowTies = false)
        val leadingTeamLines = leadingTeam?.score?.lines
            ?: state.getRegisteredTeams().maxOfOrNull { it.score.lines }
            ?: 0

        // if noone can beat or tie with the leading team, then it is a lockout (the leading team has won)
        val isLockout = when (cardGoal) {
            // this is only applicable if the goal type is Lines
            is BingoGoal.Items -> false
            is BingoGoal.Lines -> {
                state.getRegisteredTeams()
                    .filter { it.key != leadingTeam?.key }
                    .none { team ->
                        val possibleLines = card.countLines { objective -> objective.hasAchieved(team.key) || objective.hasNoneAchieved() }
                        // if a team can capture more lines, and it would either tie or beat the current team
                        possibleLines > team.score.lines && possibleLines >= leadingTeamLines
                    }
            }
        }

        val isGoalImpossible = when (cardGoal) {
            is BingoGoal.Items -> {
                state.getRegisteredTeams().none { team ->
                    val possibleItems = card.countItems { entry -> entry.hasAchieved(team.key) || entry.hasNoneAchieved() }
                    // if no team can capture the amount of items needed for the win condition
                    possibleItems >= cardGoal.items
                }
            }
            is BingoGoal.Lines -> {
                state.getRegisteredTeams().none { team ->
                    val possibleLines = card.countLines { entry -> entry.hasAchieved(team.key) || entry.hasNoneAchieved() }
                    // if no team can capture the amount of lines needed for the win condition
                    possibleLines >= cardGoal.lines
                }
            }
        }

        return isLockout || isGoalImpossible
    }

    fun getLeading(state: BingoState, card: BingoCard, allowTies: Boolean = false): BingoTeam? {
        // TODO: needs to account for # of completed cards (+ GameOverService)
        return getLeadingByLines(state, allowTies)
            ?.takeIf { card.options.goal is BingoGoal.Lines }
            ?: getLeadingByItems(state, allowTies)
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