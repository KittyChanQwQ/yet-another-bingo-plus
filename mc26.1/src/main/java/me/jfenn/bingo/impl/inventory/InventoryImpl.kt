package me.jfenn.bingo.impl.inventory

import me.jfenn.bingo.platform.inventory.IInventory
import me.jfenn.bingo.platform.item.IItemStack
import me.jfenn.bingo.platform.item.IItemStackFactory
import net.minecraft.inventory.Inventory

abstract class AbstractInventoryImpl : IInventory {
    abstract val itemStackFactory: IItemStackFactory
    abstract val inventory: Inventory

    override val size: Int
        get() = inventory.size()

    override fun get(index: Int): IItemStack? {
        if (index !in 0..<size) return null
        val stack = inventory.getStack(index)
        return itemStackFactory.forStack(stack)
    }

    override fun set(index: Int, stack: IItemStack) {
        inventory.setStack(index, stack.stack)
    }
}

class InventoryImpl(
    override val itemStackFactory: IItemStackFactory,
    override val inventory: Inventory,
) : AbstractInventoryImpl()
