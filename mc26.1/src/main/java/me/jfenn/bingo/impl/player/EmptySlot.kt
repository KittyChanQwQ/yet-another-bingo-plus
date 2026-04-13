package me.jfenn.bingo.impl.player

import net.minecraft.world.Container
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

class EmptySlot(inventory: Container, index: Int, x: Int, y: Int) : Slot(inventory, index, x, y) {
    override fun mayPlace(itemStack: ItemStack): Boolean {
        return false
    }

    override fun mayPickup(player: Player): Boolean {
        return false
    }

    override fun isActive(): Boolean {
        return false
    }
}
