package me.jfenn.bingo.common.card

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import me.jfenn.bingo.common.state.TeamScore
import net.minecraft.item.Item
import net.minecraft.util.Identifier

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
    @Transient
    val itemIndex = buildMap<Item, BingoCardEntry> {
        this@BingoCard.entries
            .filterIsInstance<BingoCardEntry.ItemEntry>()
            .forEach { entry ->
                put(entry.item(), entry)
            }

        this@BingoCard.entries
            .filterIsInstance<BingoCardEntry.MultiEntry>()
            .forEach { entry ->
                entry.innerEntries
                    .filterIsInstance<BingoCardEntry.ItemEntry>()
                    .forEach { item ->
                        put(item.item(), entry)
                    }
            }
    }

    @Transient
    val advancementIndex = buildMap<Identifier, BingoCardEntry> {
        this@BingoCard.entries
            .filterIsInstance<BingoCardEntry.AdvancementEntry>()
            .forEach { entry ->
                put(entry.identifierInstance, entry)
            }

        this@BingoCard.entries
            .filterIsInstance<BingoCardEntry.MultiEntry>()
            .forEach { entry ->
                entry.innerEntries
                    .filterIsInstance<BingoCardEntry.AdvancementEntry>()
                    .forEach { item ->
                        put(item.identifierInstance, entry)
                    }
            }
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

    inline fun <reified T: BingoCardEntry> getAllEntriesOf(): List<T> {
        return entries.filterIsInstance<BingoCardEntry.MultiEntry>()
            .map { multiEntry -> multiEntry.innerEntries.filterIsInstance<T>() }
            .flatten()
            .plus(entries.filterIsInstance<T>())
    }

}