package me.jfenn.bingo.common.card

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import me.jfenn.bingo.common.NBT_BINGO_IGNORE
import me.jfenn.bingo.common.config.TierList
import me.jfenn.bingo.common.utils.text.TextColor
import me.jfenn.bingo.common.utils.text.TextInfo
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import kotlin.time.Duration

@Serializable
sealed class BingoCardEntry {

    open val teams = HashSet<BingoTeamKey>()
    private val teamsSeen = HashSet<BingoTeamKey>()

    abstract val visualItem: Item?

    abstract val tier: TierList?

    open fun formatName(): String? {
        return null
    }

    open fun formatLore(): List<String> {
        return emptyList()
    }

    open fun hasAchieved(team: BingoTeam): Boolean {
        return teams.contains(team.key)
    }

    /**
     * @return true if the tile state has changed
     */
    open fun setAchieved(team: BingoTeam, value: Boolean): Boolean {
        return when {
            value -> teams.add(team.key)
            else -> teams.remove(team.key)
        }.also { changed ->
            if (changed) markDirty(team)
        }
    }

    open fun hasAnyAchieved() = teams.isNotEmpty()
    fun hasNoneAchieved() = !hasAnyAchieved()

    /**
     * Checks if the [team] has seen the item on
     * the current tile (regardless of whether it can be scored)
     */
    open fun hasSeen(team: BingoTeam): Boolean {
        return teamsSeen.contains(team.key)
    }

    /**
     * Marks the tile as seen by the provided [team].
     */
    open fun markSeen(team: BingoTeam) {
        if (teamsSeen.add(team.key)) {
            markDirty(team)
        }
    }

    @Transient
    private val updatedAtMap = mutableMapOf<BingoTeamKey?, Instant>()

    val updatedAt: Instant get() = updatedAt(null)

    /**
     * Determines the last time that the tile was changed at.
     * If [team] is not null, this returns the last changed time
     * for the specified team.
     */
    open fun updatedAt(team: BingoTeam?): Instant = updatedAtMap.getOrPut(team?.key) { Clock.System.now() }

    /**
     * Marks the tile as changed, indicating that it needs to be redrawn on
     * the bingo map
     */
    open fun markDirty(team: BingoTeam) {
        Clock.System.now().let {
            updatedAtMap[team.key] = it
            updatedAtMap[null] = it
        }
    }

    /**
     * Returns when the tile is flashing on the map, based on its update time
     * - i.e. if the item has been updated within [duration]
     */
    open fun isFlashing(team: BingoTeam?, duration: Duration): Boolean {
        return team != null &&
                hasAchieved(team) &&
                Clock.System.now() - updatedAt(team) < duration
    }

    @Serializable
    class FreeSpace : BingoCardEntry() {
        @Transient
        override val visualItem: Item? = null

        @Transient
        override val tier: TierList? = null
        override fun hasAnyAchieved(): Boolean = true
        override fun hasAchieved(team: BingoTeam): Boolean = true
        override fun setAchieved(team: BingoTeam, value: Boolean): Boolean = false
        override fun isFlashing(team: BingoTeam?, duration: Duration): Boolean = false

        override fun formatName(): String {
            return TextInfo.toJsonString(
                TextInfo.literal("Free Space")
            )
        }
    }

    @Serializable
    data class ItemEntry(
        val identifier: String,
        override val tier: TierList,
    ): BingoCardEntry() {

        @Transient
        override val visualItem: Item = Registries.ITEM.get(Identifier(identifier))

        /**
         * Returns true if the item can be counted to complete the bingo tile
         *
         * Returns false if any of:
         * - item does not match the card item
         * - item has a bingo card's map ID
         * - item has a "bingo_ignore:true" NBT tag
         */
        fun canSatisfy(stack: ItemStack): Boolean {
            return stack.item == item() && stack.nbt?.getBoolean(NBT_BINGO_IGNORE) != true
        }

        fun item(): Item {
            return visualItem
        }

    }

    @Serializable
    data class MultiEntry(
        val innerEntries: List<BingoCardEntry>,
        override val tier: TierList,
    ): BingoCardEntry() {
        override val visualItem: Item? get() = innerEntries.firstOrNull()?.visualItem

        override val teams: HashSet<BingoTeamKey>
            get() = HashSet<BingoTeamKey>().apply {
                for (entry in innerEntries)
                    addAll(entry.teams)
            }

        override fun formatName(): String {
            return TextInfo.toJsonString(
                TextInfo.literal("One of:")
                    .formatted(TextColor.GRAY)
            )
        }

        override fun formatLore(): List<String> {
            return innerEntries
                .mapNotNull { it.visualItem?.translationKey }
                .map { "{\"translate\":\"$it\"}" }
        }

        override fun hasAchieved(team: BingoTeam): Boolean {
            return innerEntries.any { it.hasAchieved(team) }
        }

        @Throws(UnsupportedOperationException::class)
        override fun setAchieved(team: BingoTeam, value: Boolean): Boolean {
            throw UnsupportedOperationException("setAchieved on MultiEntry")
        }

        override fun hasSeen(team: BingoTeam): Boolean {
            return innerEntries.any { it.hasSeen(team) }
        }

        @Throws(UnsupportedOperationException::class)
        override fun markSeen(team: BingoTeam) {
            throw UnsupportedOperationException("markSeen on MultiEntry")
        }

        override fun updatedAt(team: BingoTeam?): Instant {
            if (team != null && hasAchieved(team)) {
                innerEntries
                    .filter { it.hasAchieved(team) }
                    .minOfOrNull { it.updatedAt(team) }
                    ?.let { return it }
            }

            return innerEntries
                .maxOfOrNull { it.updatedAt(team) }
                ?: Instant.fromEpochMilliseconds(0L)
        }

        @Throws(UnsupportedOperationException::class)
        override fun markDirty(team: BingoTeam) {
            throw UnsupportedOperationException("markDirty on MultiEntry")
        }
    }
}
