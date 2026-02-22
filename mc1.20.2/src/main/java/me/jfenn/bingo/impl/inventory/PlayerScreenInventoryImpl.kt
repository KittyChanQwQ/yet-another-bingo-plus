package me.jfenn.bingo.impl.inventory

import me.jfenn.bingo.platform.inventory.IInventory
import me.jfenn.bingo.platform.item.IItemStack
import me.jfenn.bingo.platform.item.IItemStackFactory
import net.minecraft.server.network.ServerPlayerEntity

class PlayerScreenInventoryImpl(
    private val itemStackFactory: IItemStackFactory,
    private val player: ServerPlayerEntity,
) : IInventory {
    override val size: Int
        get() = 1

    override fun get(index: Int): IItemStack? {
        if (index == 0) {
            // yield the current cursor stack
            return itemStackFactory.forStack(player.currentScreenHandler.cursorStack)
        }

        return null
    }

    override fun set(index: Int, stack: IItemStack) {
        if (index == 0) {
            player.currentScreenHandler.cursorStack = stack.stack
        }
    }
}
