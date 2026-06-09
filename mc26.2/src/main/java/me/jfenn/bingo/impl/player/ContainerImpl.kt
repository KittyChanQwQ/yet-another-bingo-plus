package me.jfenn.bingo.impl.player

import me.jfenn.bingo.platform.inventory.IInventory
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

class ContainerImpl(
    private val inventory: IInventory,
) : Container {
    override fun clearContent() {
    }

    override fun getContainerSize(): Int {
        return inventory.size
    }

    override fun isEmpty(): Boolean {
        return false
    }

    override fun getItem(slot: Int): ItemStack {
        return inventory[slot]?.stack ?: ItemStack.EMPTY
    }

    override fun removeItem(slot: Int, count: Int): ItemStack {
        return ItemStack.EMPTY
    }

    override fun removeItemNoUpdate(slot: Int): ItemStack {
        return ItemStack.EMPTY
    }

    override fun setItem(slot: Int, itemStack: ItemStack) {
    }

    override fun setChanged() {
    }

    override fun stillValid(player: Player): Boolean {
        return true
    }
}
