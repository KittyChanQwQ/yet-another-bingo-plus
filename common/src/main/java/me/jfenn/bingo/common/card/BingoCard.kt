package me.jfenn.bingo.common.card

import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.state.TeamScore

@Serializable
class BingoCard(
    val entries: List<BingoCardEntry>,
) {

    init {
        assert(entries.size == 25) { "BINGO card was created with less than 25 entries! (${entries.size})" }
    }

    /**
     * Index the card's item entries on their item for faster querying.
     */
    val itemIndex by lazy {
        getAllItemEntries().associateBy { it.item() }
    }

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
                .all(predicate)

            if (isLine) lines++
        }

        for (col in 0..4) {
            val isLine = (0..4)
                .map { entry(it, col) }
                .all(predicate)

            if (isLine) lines++
        }

        val isD1 = (0..4)
            .map { entry(it, it) }
            .all(predicate)

        if (isD1) lines++

        val isD2 = (0..4)
            .map { entry(it, 4 - it) }
            .all(predicate)

        if (isD2) lines++

        return lines
    }

    /**
     * Counts every horizontal, vertical, and diagonal line on the card
     * achieved by the given [team].
     */
    fun countLines(team: BingoTeam): Int = countLines { it.hasAchieved(team) }

    /**
     * Counts the total number of items achieved by the given [team].
     */
    fun countItems(team: BingoTeam): Int = entries.count { it.hasAchieved(team) }

    fun getTeamScore(team: BingoTeam): TeamScore {
        return TeamScore(
            items = countItems(team),
            lines = countLines(team),
        )
    }

    fun getAllItemEntries(): List<BingoCardEntry.ItemEntry> {
        val multiEntries = entries.filterIsInstance<BingoCardEntry.MultiEntry>()
            .map { multiEntry -> multiEntry.innerEntries.filterIsInstance<BingoCardEntry.ItemEntry>() }
            .flatten()

        return multiEntries + entries.filterIsInstance<BingoCardEntry.ItemEntry>()
    }

}