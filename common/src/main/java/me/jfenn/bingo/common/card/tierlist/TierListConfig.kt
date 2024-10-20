package me.jfenn.bingo.common.card.tierlist

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
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
    val name: JsonElement? = null,
    val groups: List<Set<String>> = emptyList(),
    val requirements: Requirements = Requirements(),
) {

    @Transient
    var shouldValidate = true

    @Serializable
    class Requirements(
        val minTeams: Int = 0,
        val maxTeams: Int = Int.MAX_VALUE,
        val minPlayersPerTeam: Int = 0,
        val maxPlayersPerTeam: Int = Int.MAX_VALUE,
        val objectives: Set<String> = emptySet(),
        val onlyDefaultTeams: Boolean = false,
        val options: JsonObject = JsonObject(emptyMap()),
    ) {
        val numTeams get() = minTeams..maxTeams
        val numPlayersPerTeam get() = minPlayersPerTeam..maxPlayersPerTeam
    }

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
        return this.copy(
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
                .filter { group -> group.contains(entry.item) }
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
            if (excluding.contains(item.item)) continue

            // if the chosen item is in any item groups, exclude all of those groups
            excluding.add(item.item)
            for (group in groups.filter { group -> group.contains(item.item) }) {
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

    fun expandName(
        listName: String,
    ) : TierListConfig {
        return this.copy(
            s = this.s.map { it.copy(listName = listName) }.toSet(),
            a = this.a.map { it.copy(listName = listName) }.toSet(),
            b = this.b.map { it.copy(listName = listName) }.toSet(),
            c = this.c.map { it.copy(listName = listName) }.toSet(),
            d = this.d.map { it.copy(listName = listName) }.toSet(),
        )
    }

    private fun expandTags(
        expansionService: TagExpansionService,
        entry: TierListEntry,
    ): List<TierListEntry> {
        return expansionService.expandItemTag(entry.item).map { TierListEntry(it) }
    }

}