package me.jfenn.bingo.data

class BingoCard(
    val entries: List<BingoCardEntry>,
) {

    fun entry(x: Int, y: Int): BingoCardEntry {
        return entries[x + y * 5]
    }

    fun items(): Sequence<Pair<Pair<Int, Int>, BingoCardEntry>> = sequence {
        for (i in entries.indices) {
            val x = i % 5
            val y = i / 5
            yield(Pair(Pair(x, y), entries[i]))
        }
    }

    fun countLines(team: BingoTeam): Int {
        var lines = 0

        for (row in 0..4) {
            val isLine = (0..4)
                .map { entry(row, it) }
                .all { it.hasAchieved(team) }

            if (isLine) lines++
        }

        for (col in 0..4) {
            val isLine = (0..4)
                .map { entry(it, col) }
                .all { it.hasAchieved(team) }

            if (isLine) lines++
        }

        val isD1 = (0..4)
            .map { entry(it, it) }
            .all { it.hasAchieved(team) }

        if (isD1) lines++

        val isD2 = (0..4)
            .map { entry(it, 4 - it) }
            .all { it.hasAchieved(team) }

        if (isD2) lines++

        return lines
    }

}