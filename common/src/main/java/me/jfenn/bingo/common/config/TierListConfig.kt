package me.jfenn.bingo.common.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class TierListConfig(
    @SerialName("S")
    val s: Set<TierListEntry>,
    @SerialName("A")
    val a: Set<TierListEntry>,
    @SerialName("B")
    val b: Set<TierListEntry>,
    @SerialName("C")
    val c: Set<TierListEntry>,
    @SerialName("D")
    val d: Set<TierListEntry>,
    val groups: List<Set<String>>,
) {

    companion object {
        val EMPTY = TierListConfig(
            s = emptySet(),
            a = emptySet(),
            b = emptySet(),
            c = emptySet(),
            d = emptySet(),
            groups = emptyList(),
        )
    }

    fun combine(other: TierListConfig): TierListConfig {
        return TierListConfig(
            s = s + other.s,
            a = a + other.a,
            b = b + other.b,
            c = c + other.c,
            d = d + other.d,
            groups = groups + other.groups,
        )
    }

    fun pick(distribution: List<Int>, excludingItems: Set<String>, random: Random): List<TierListEntry> {
        val excluding = excludingItems.toMutableSet()
        val ret = mutableListOf<TierListEntry>()

        // for each tier...
        for ((i, items) in listOf(s, a, b, c, d).withIndex()) {
            // get the expected number of items
            val count = distribution.getOrNull(i) ?: 0

            // choose random items up to the expected count...
            var countAdded = 0
            for (item in items.shuffled(random)) {
                if (countAdded >= count) break
                // skip excluded items
                if (item.asList().any { excluding.contains(it) }) continue

                // add the item to the list
                ret.add(item)
                countAdded++

                // if the chosen item is in any item groups, exclude all of those groups
                excluding.addAll(item.asList())
                for (group in groups.filter { group -> item.asList().any { group.contains(it) } }) {
                    excluding.addAll(group)
                }
            }
        }

        return ret
    }

}