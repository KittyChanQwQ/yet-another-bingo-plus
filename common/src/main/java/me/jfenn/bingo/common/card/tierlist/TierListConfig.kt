package me.jfenn.bingo.common.card.tierlist

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.card.TagExpansionService
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

    fun expandTags(
        expansionService: TagExpansionService,
    ) : TierListConfig {
        return this.copy(
            s = this.s.flatMap { expandTags(expansionService, it) }.toSet(),
            a = this.a.flatMap { expandTags(expansionService, it) }.toSet(),
            b = this.b.flatMap { expandTags(expansionService, it) }.toSet(),
            c = this.c.flatMap { expandTags(expansionService, it) }.toSet(),
            d = this.d.flatMap { expandTags(expansionService, it) }.toSet(),
            groups = this.groups.map { expansionService.expandItemTags(it) },
        )
    }

    private fun expandTags(
        expansionService: TagExpansionService,
        entry: TierListEntry,
    ): List<TierListEntry> {
        return when (entry) {
            is TierListEntry.SingleEntry -> expansionService.expandItemTag(entry.item).map { TierListEntry.SingleEntry(it) }
        }
    }

}