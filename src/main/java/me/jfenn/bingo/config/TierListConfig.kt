package me.jfenn.bingo.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TierListConfig(
    @SerialName("S")
    val s: List<String>,
    @SerialName("A")
    val a: List<String>,
    @SerialName("B")
    val b: List<String>,
    @SerialName("C")
    val c: List<String>,
    @SerialName("D")
    val d: List<String>,
    val groups: List<Set<String>>,
) {

    companion object {
        val EMPTY = TierListConfig(
            s = emptyList(),
            a = emptyList(),
            b = emptyList(),
            c = emptyList(),
            d = emptyList(),
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

    fun pick(distribution: List<Int>, excludingItems: Set<String>): List<String> {
        val excluding = excludingItems.toMutableSet()
        val ret = mutableListOf<String>()

        // for each tier...
        for ((i, items) in listOf(s, a, b, c, d).withIndex()) {
            // get the expected number of items
            val count = distribution.getOrNull(i) ?: 0

            // choose random items up to the expected count...
            var countAdded = 0
            for (item in items.shuffled()) {
                if (countAdded >= count) break
                // skip excluded items
                if (excluding.contains(item)) continue

                // if the chosen item is in any item groups, exclude all of those groups
                for (group in groups.filter { it.contains(item) }) {
                    excluding.addAll(group)
                }

                ret.add(item)
                countAdded++
            }
        }

        return ret
    }

}