package me.jfenn.bingo.card

import me.jfenn.bingo.team.BingoTeam

class BingoCard(
    val entries: List<BingoCardEntry>,
) {

    fun entry(x: Int, y: Int): BingoCardEntry {
        return entries[x + y * 5]
    }

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

    fun countLines(team: BingoTeam): Int = countLines { it.hasAchieved(team) }

}