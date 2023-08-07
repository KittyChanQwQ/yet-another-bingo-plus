package me.jfenn.bingo.generator

import me.jfenn.bingo.data.BingoCard
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

class CardInventory(
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

    override fun getStack(slot: Int): ItemStack {
        val slotX = (slot % 9) - 2
        val slotY = slot / 9

        if (slotX in 0 until 5 && slotY in 0 until 5) {
            val entry = card.entry(slotX, slotY)
            val item = Registries.ITEM[Identifier.of("minecraft", entry.item.lowercase())]
            return ItemStack(item, 1)
        }

        return ItemStack.EMPTY
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