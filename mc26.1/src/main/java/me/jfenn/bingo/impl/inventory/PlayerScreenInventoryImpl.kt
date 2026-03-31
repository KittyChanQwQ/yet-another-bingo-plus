package me.jfenn.bingo.impl.inventory

import me.jfenn.bingo.platform.inventory.IInventory
import me.jfenn.bingo.platform.item.IItemStack
import me.jfenn.bingo.platform.item.IItemStackFactory
import net.minecraft.screen.AbstractCraftingScreenHandler
import net.minecraft.server.network.ServerPlayerEntity

class PlayerScreenInventoryImpl(
    private val itemStackFactory: IItemStackFactory,
    private val player: ServerPlayerEntity,
) : IInventory {
    override val size: Int
        get() = 1 + (
                (player.currentScreenHandler as? AbstractCraftingScreenHandler)?.inputSlots?.size ?: 0
        )

    override fun get(index: Int): IItemStack? {
        if (index == 0) {
            // yield the current cursor stack
            return itemStackFactory.forStack(player.currentScreenHandler.cursorStack)
        }

        // yield items held in crafting inputs
        val screenHandler = player.currentScreenHandler
        if (screenHandler is AbstractCraftingScreenHandler) {
            val slot = screenHandler.inputSlots[index - 1]
            return itemStackFactory.forStack(slot.stack)
        }

        return null
    }

    override fun set(index: Int, stack: IItemStack) {
        if (index == 0) {
            player.currentScreenHandler.cursorStack = stack.stack
            return
        }

        val screenHandler = player.currentScreenHandler
        if (screenHandler is AbstractCraftingScreenHandler) {
            screenHandler.inputSlots[index - 1].stack = stack.stack
        }
    }
}
