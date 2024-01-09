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

@Serializable
sealed class BingoCardEntry {

    val teams = HashSet<BingoTeam>()

    abstract val visualItem: Item?

    open fun formatName(): Text? {
        return visualItem?.name
    }

    open fun hasAchieved(team: BingoTeam): Boolean {
        return teams.contains(team)
    }

    /**
     * @return true if the tile state has changed
     */
    open fun setAchieved(team: BingoTeam, value: Boolean): Boolean {
        return when {
            value -> teams.add(team)
            else -> teams.remove(team)
        }.also { changed ->
            if (changed) markDirty(team)
        }
    }

    fun hasAnyAchieved() = teams.isNotEmpty()
    fun hasNoneAchieved() = teams.isEmpty()

    private val updatedAtMap = mutableMapOf<BingoTeam?, Instant>()

    val updatedAt: Instant get() = updatedAt(null)

    fun updatedAt(team: BingoTeam?): Instant = updatedAtMap.getOrPut(team) { Clock.System.now() }

    fun markDirty(team: BingoTeam) {
        Clock.System.now().let {
            updatedAtMap[team] = it
            updatedAtMap[null] = it
        }
    }

    @Serializable
    class FreeSpace : BingoCardEntry() {
        init {
            teams.addAll(BingoTeam.entries)
        }

        @Transient
        override val visualItem: Item? = null
        override fun hasAchieved(team: BingoTeam): Boolean = true
        override fun setAchieved(team: BingoTeam, value: Boolean): Boolean = false
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
