package me.jfenn.bingo.card

import me.jfenn.bingo.team.BingoTeam

enum class WinCondition(val numLines: Int) {
    ONE_LINE(1),
    TWO_LINES(2),
    THREE_LINES(3),
    FULL_CARD(12);

    fun getWinner(card: BingoCard): BingoTeam? {
        for (team in BingoTeam.values()) {
            val lines = card.countLines(team)
            if (lines >= numLines) return team
        }

        return null
    }
}