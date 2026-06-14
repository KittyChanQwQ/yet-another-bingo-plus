package me.jfenn.bingo.impl.inventory

import me.jfenn.bingo.platform.inventory.IInventory
import me.jfenn.bingo.platform.item.IItemStack
import me.jfenn.bingo.platform.item.IItemStackFactory
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.inventory.AbstractCraftingMenu

class PlayerScreenInventoryImpl(
    private val itemStackFactory: IItemStackFactory,
    private val player: ServerPlayer,
) : IInventory {
    override val size: Int
        get() = 1 + (
                (player.containerMenu as? AbstractCraftingMenu)?.inputGridSlots?.size ?: 0
        )

    override fun get(index: Int): IItemStack? {
        if (index == 0) {
            // yield the current cursor stack
            return itemStackFactory.forStack(player.containerMenu.carried)
        }

        // yield items held in crafting inputs
        val screenHandler = player.containerMenu
        if (screenHandler is AbstractCraftingMenu) {
            val slot = screenHandler.inputGridSlots[index - 1]
            return itemStackFactory.forStack(slot.item)
        }

        return null
    }

    override fun set(index: Int, stack: IItemStack) {
        if (index == 0) {
            player.containerMenu.carried = stack.stack
            return
        }

        val screenHandler = player.containerMenu
        if (screenHandler is AbstractCraftingMenu) {
            screenHandler.inputGridSlots[index - 1].setByPlayer(stack.stack)
        }
    }
}
