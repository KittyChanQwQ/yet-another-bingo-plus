package me.jfenn.bingo.card

import me.jfenn.bingo.NBT_BINGO_IGNORE
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

abstract class BingoCardEntry {

    val teams = HashSet<BingoTeam>()

    abstract val visualItem: Item?

    open fun hasAchieved(team: BingoTeam): Boolean {
        return teams.contains(team)
    }

    /**
     * @return true if the tile state has changed
     */
    open fun setAchieved(team: BingoTeam, value: Boolean): Boolean {
        return if (value) {
            teams.add(team)
        } else {
            teams.remove(team)
        }
    }

    fun hasAnyAchieved() = teams.isNotEmpty()
    fun hasNoneAchieved() = teams.isEmpty()

}

object FreeSpaceEntry : BingoCardEntry() {

    init {
        teams.addAll(BingoTeam.entries)
    }

    override val visualItem: Item? = null
    override fun hasAchieved(team: BingoTeam): Boolean = true
    override fun setAchieved(team: BingoTeam, value: Boolean): Boolean = false
}

class ItemEntry(
    identifier: Identifier,
): BingoCardEntry() {

    override val visualItem: Item = Registries.ITEM.get(identifier)

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
