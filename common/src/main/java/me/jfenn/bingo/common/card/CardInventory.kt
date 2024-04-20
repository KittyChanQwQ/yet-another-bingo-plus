package me.jfenn.bingo.common.card

import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.utils.text.TextInfo
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtString

class CardInventory(
    val options: BingoOptions,
    val state: BingoState,
    val team: BingoTeam?,
    val card: BingoCard,
) : Inventory {
    override fun clear() {
    }

    override fun size(): Int {
        return 25
    }

    override fun isEmpty(): Boolean {
        return false
    }

    val inventory = List(45) { slot ->
        val slotX = (slot % 9) - 2
        val slotY = slot / 9

        if (slotX in 0 until 5 && slotY in 0 until 5) {
            val entry = card.entry(slotX, slotY)

            // If the items are hidden, don't show them in the card inventory!
            val isVisible = !options.isHiddenItemsMode ||
                    // if the team isn't null and has seen the item
                    (team != null && entry.hasSeen(team)) ||
                    // if the team is null and the game is playing (i.e. the player is spectating)
                    (team == null && state.state == GameState.PLAYING) ||
                    // if the game is ended
                    state.state == GameState.POSTGAME

            if (isVisible) {
                entry.visualItem()?.let {
                    return@List it.stack
                }
            }
        }

        ItemStack.EMPTY
    }

    override fun getStack(slot: Int): ItemStack {
        return inventory.getOrNull(slot) ?: ItemStack.EMPTY
    }

    override fun removeStack(slot: Int, amount: Int): ItemStack {
        return ItemStack.EMPTY
    }

    override fun removeStack(slot: Int): ItemStack {
        return ItemStack.EMPTY
    }

    override fun setStack(slot: Int, stack: ItemStack?) {
    }

    override fun markDirty() {
    }

    override fun canPlayerUse(player: PlayerEntity?): Boolean {
        return true
    }
}