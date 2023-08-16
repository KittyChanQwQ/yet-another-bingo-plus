package me.jfenn.bingo.config

import me.jfenn.bingo.card.BingoCard
import me.jfenn.bingo.team.BingoTeam

enum class WinCondition(val numLines: Int) {
    ONE_LINE(1),
    TWO_LINES(2),
    THREE_LINES(3),
    FULL_CARD(12);

    fun getLeading(card: BingoCard): BingoTeam? {
        var max: BingoTeam? = null
        var maxLines: Int = 0
        for (team in BingoTeam.values()) {
           val teamLines = card.countLines(team)
           if (teamLines > maxLines) {
               max = team
               maxLines = teamLines
           } else if (teamLines == maxLines) {
               max = null
           }
        }

        return max
    }

    fun getWinner(card: BingoCard): BingoTeam? {
        for (team in BingoTeam.values()) {
            val lines = card.countLines(team)
            if (lines >= numLines) return team
        }

        return null
    }

    fun isStalemate(card: BingoCard, config: BingoConfig): Boolean {
        if (config.gameMode != BingoMode.LOCKOUT) return false

        // check all teams against the number of lines needed to win
        // if any team can win the game, it is not a stalemate
        for (team in BingoTeam.values()) {
            // count number of lines that the team can achieve
            val possibleLines = card.countLines { entry -> entry.teams.isEmpty() || entry.hasAchieved(team) }
            if (possibleLines >= numLines) return false
        }

        return true
    }
}