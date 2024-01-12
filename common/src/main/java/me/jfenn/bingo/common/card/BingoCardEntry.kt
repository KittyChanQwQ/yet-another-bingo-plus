package me.jfenn.bingo.common.card

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import me.jfenn.bingo.common.NBT_BINGO_IGNORE
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import kotlin.time.Duration

@Serializable
sealed class BingoCardEntry {

    val teams = HashSet<BingoTeamKey>()
    val teamsSeen = HashSet<BingoTeamKey>()

    abstract val visualItem: Item?

    open fun formatName(): Text? {
        return visualItem?.name
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
    fun updatedAt(team: BingoTeam?): Instant = updatedAtMap.getOrPut(team?.key) { Clock.System.now() }

    /**
     * Marks the tile as changed, indicating that it needs to be redrawn on
     * the bingo map
     */
    fun markDirty(team: BingoTeam) {
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
        override fun hasAnyAchieved(): Boolean = true
        override fun hasAchieved(team: BingoTeam): Boolean = true
        override fun setAchieved(team: BingoTeam, value: Boolean): Boolean = false
        override fun isFlashing(team: BingoTeam?, duration: Duration): Boolean = false

        override fun formatName(): Text? {
            return Text.literal("Free Space")
        }
    }

    @Serializable
    data class ItemEntry(
        val identifier: String,
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
}
