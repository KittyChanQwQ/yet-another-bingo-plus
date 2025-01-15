package me.jfenn.bingo.common.card

import me.jfenn.bingo.common.map.CardView
import me.jfenn.bingo.platform.item.IItemStackFactory
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack

class CardInventory(
    val view: CardView,
    val itemStackFactory: IItemStackFactory,
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

        if (slotX !in 0 until 5 || slotY !in 0 until 5)
            return@List ItemStack.EMPTY

        val tile = view.tiles.getOrNull(slotX + slotY * 5)
            ?: return@List ItemStack.EMPTY

        if (tile.isHidden) {
            return@List ItemStack.EMPTY
        }

        val tileItem = tile.image.item
        val stack = when {
            tile.isLocked -> kotlin.runCatching { itemStackFactory.createStack("minecraft:barrier") }
                .getOrNull()
                ?.apply {
                    setDisplay(tile.name, tile.lore)
                    setHideFlags(255)
                } ?: tileItem
            tileItem != null -> tileItem
            else -> {
                kotlin.runCatching { itemStackFactory.createStack("minecraft:knowledge_book") }
                    .getOrNull()
                    ?.apply {
                        setDisplay(tile.name, tile.lore)
                        setHideFlags(255)
                    }
            }
        }

        stack?.stack ?: ItemStack.EMPTY
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