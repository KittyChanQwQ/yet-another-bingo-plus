package me.jfenn.bingo.card

import me.jfenn.bingo.config.BingoConfig
import me.jfenn.bingo.config.BingoMode

class BingoCard(
    val entries: List<BingoCardEntry>,
) {

    /**
     * Index the card entries on their item for faster querying.
     */
    val itemIndex by lazy { entries.associateBy { it.item() } }

    /**
     * Gets the card entry at the provided x/y index.
     *
     * Throws if [x] or [y] arguments are not within 0..4
     */
    fun entry(x: Int, y: Int): BingoCardEntry {
        assert(x in 0 until 5) { "x=$x is not within the required 0..4 range" }
        assert(y in 0 until 5) { "y=$y is not within the required 0..4 range" }

        return entries[x + y * 5]
    }

    /**
     * Counts every horizontal, vertical, and diagonal line on the card
     * satisfied by the [predicate], and returns the sum.
     */
    fun countLines(predicate: (BingoCardEntry) -> Boolean): Int {
        var lines = 0

        for (row in 0..4) {
            val isLine = (0..4)
                .map { entry(row, it) }
                .all { predicate(it) }

            if (isLine) lines++
        }

        for (col in 0..4) {
            val isLine = (0..4)
                .map { entry(it, col) }
                .all { predicate(it) }

            if (isLine) lines++
        }

        val isD1 = (0..4)
            .map { entry(it, it) }
            .all { predicate(it) }

        if (isD1) lines++

        val isD2 = (0..4)
            .map { entry(it, 4 - it) }
            .all { predicate(it) }

        if (isD2) lines++

        return lines
    }

    /**
     * Counts every horizontal, vertical, and diagonal line on the card
     * achieved by the given [team].
     */
    fun countLines(team: BingoTeam): Int = countLines { it.hasAchieved(team) }

    fun getLeading(): BingoTeam? {
        var max: BingoTeam? = null
        var maxLines = 0
        for (team in BingoTeam.values()) {
            val teamLines = countLines(team)
            if (teamLines > maxLines) {
                max = team
                maxLines = teamLines
            } else if (teamLines == maxLines) {
                max = null
            }
        }

        return max
    }

    fun getWinner(config: BingoConfig): BingoTeam? {
        for (team in BingoTeam.values()) {
            val lines = countLines(team)
            if (lines >= config.winCondition.numLines) return team
        }

        return null
    }

    fun isStalemate(config: BingoConfig): Boolean {
        if (config.gameMode != BingoMode.LOCKOUT) return false

        // check all teams for the number of capture-able lines
        // if any team can capture a line, it is not a stalemate
        for (team in BingoTeam.values()) {
            // count number of lines that the team can achieve
            val possibleLines = countLines { entry -> entry.teams.isEmpty() || entry.hasAchieved(team) }
            if (possibleLines >= 1) return false
        }

        return true
    }

}