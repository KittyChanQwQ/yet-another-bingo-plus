package me.jfenn.bingo.common.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*
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

        const val ADVANCEMENT_PREFIX = "advancement!"
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

    fun getTier(tier: TierLabel) = when (tier) {
        TierLabel.S -> s
        TierLabel.A -> a
        TierLabel.B -> b
        TierLabel.C -> c
        TierLabel.D -> d
    }

    private fun getWeightedSet(set: Set<TierListEntry>): LinkedList<Pair<TierListEntry, Double>> {
        val ret = LinkedList<Pair<TierListEntry, Double>>()
        for (entry in set) {
            val inverseWeight = groups
                .filter { group -> entry.asList().any { group.contains(it) } }
                .sumOf { group -> group.size }
                .coerceAtLeast(1)

            ret.add(entry to (1.0 / inverseWeight))
        }

        return ret
    }

    fun pick(tier: TierLabel, excluding: MutableSet<String>, random: Random) = sequence {
        val items = getTier(tier)
        val itemWeights = getWeightedSet(items)

        while (itemWeights.isNotEmpty()) {
            val itemWeightTotal = itemWeights.sumOf { it.second }
            val choice = random.nextDouble(0.0, itemWeightTotal)

            // get the index of the random weight in the tier list
            var i = 0
            var itemWeightSum = 0.0
            for ((_, weight) in itemWeights) {
                itemWeightSum += weight
                if (itemWeightSum >= choice)
                    break

                i++
            }

            i = i.coerceAtMost(itemWeights.size - 1)
            val (item, _) = itemWeights.removeAt(i)
            // skip excluded items
            if (item.asList().any { excluding.contains(it) }) continue

            // if the chosen item is in any item groups, exclude all of those groups
            excluding.addAll(item.asList())
            for (group in groups.filter { group -> item.asList().any { group.contains(it) } }) {
                excluding.addAll(group)
            }

            // add the item to the list
            yield(item)
        }
    }

    fun pick(distribution: List<Int>, excludingItems: Set<String>, random: Random): List<Pair<TierListEntry, TierLabel>> {
        val excluding = excludingItems.toMutableSet()
        val ret = mutableListOf<Pair<TierListEntry, TierLabel>>()

        // for each tier...
        for ((tierIndex, tier) in TierLabel.entries.withIndex()) {
            // get the expected number of items
            val count = distribution.getOrNull(tierIndex) ?: 0
            val items = getTier(tier)

            val itemWeights = getWeightedSet(items)

            // choose random items up to the expected count...
            var countAdded = 0
            while (countAdded < count && itemWeights.isNotEmpty()) {
                val itemWeightTotal = itemWeights.sumOf { it.second }
                val choice = random.nextDouble(0.0, itemWeightTotal)

                // get the index of the random weight in the tier list
                var i = 0
                var itemWeightSum = 0.0
                for ((_, weight) in itemWeights) {
                    itemWeightSum += weight
                    if (itemWeightSum >= choice)
                        break

                    i++
                }

                i = i.coerceAtMost(itemWeights.size - 1)
                val (item, _) = itemWeights.removeAt(i)
                // skip excluded items
                if (item.asList().any { excluding.contains(it) }) continue

                // add the item to the list
                ret.add(item to tier)
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