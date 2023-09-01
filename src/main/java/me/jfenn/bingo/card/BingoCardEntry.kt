package me.jfenn.bingo.card

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

data class BingoCardEntry(
    val item: String,
) {

    val identifier get() = Identifier.of("minecraft", item.lowercase())!!

    val teams = HashSet<BingoTeam>()

    /**
     * Returns true if the item can be counted to complete the bingo tile
     *
     * Returns false if any of:
     * - item does not match the card item
     * - item has a bingo card's map ID
     * - item has a "bingo_ignore:true" NBT tag
     */
    fun canSatisfy(stack: ItemStack): Boolean {
        return stack.item == item() && stack.nbt?.getBoolean("bingo_ignore") != true
    }

    fun hasAchieved(team: BingoTeam): Boolean {
        return teams.contains(team)
    }

    /**
     * @return true if the tile state has changed
     */
    fun setAchieved(team: BingoTeam, value: Boolean): Boolean {
        return if (value) {
            teams.add(team)
        } else {
            teams.remove(team)
        }
    }

    fun item(): Item {
        return Registries.ITEM.get(identifier)
    }

}
