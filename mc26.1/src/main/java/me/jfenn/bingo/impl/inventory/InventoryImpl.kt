package me.jfenn.bingo.impl.inventory

import me.jfenn.bingo.platform.inventory.IInventory
import me.jfenn.bingo.platform.item.IItemStack
import me.jfenn.bingo.platform.item.IItemStackFactory
import net.minecraft.world.Container

abstract class AbstractInventoryImpl : IInventory {
    abstract val itemStackFactory: IItemStackFactory
    abstract val inventory: Container

    override val size: Int
        get() = inventory.containerSize

    override fun get(index: Int): IItemStack? {
        if (index !in 0..<size) return null
        val stack = inventory.getItem(index)
        return itemStackFactory.forStack(stack)
    }

    override fun set(index: Int, stack: IItemStack) {
        inventory.setItem(index, stack.stack)
    }
}

class InventoryImpl(
    override val itemStackFactory: IItemStackFactory,
    override val inventory: Container,
) : AbstractInventoryImpl()
