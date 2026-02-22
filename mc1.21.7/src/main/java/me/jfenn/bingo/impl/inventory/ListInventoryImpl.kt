package me.jfenn.bingo.impl.inventory

import me.jfenn.bingo.platform.inventory.IInventory
import me.jfenn.bingo.platform.item.IItemStack

class ListInventoryImpl(
    list: List<IItemStack>,
) : IInventory {
    private val list: MutableList<IItemStack> = list.toMutableList()

    override val size: Int = list.size

    override fun get(index: Int): IItemStack? = list.getOrNull(index)

    override fun set(index: Int, stack: IItemStack) {
        list[index] = stack
    }
}
