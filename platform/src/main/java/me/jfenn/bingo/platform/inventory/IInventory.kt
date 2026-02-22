package me.jfenn.bingo.platform.inventory

import me.jfenn.bingo.platform.item.IItemStack
import net.minecraft.inventory.Inventory

interface IInventory: Collection<IItemStack> {
    override val size: Int
    operator fun get(index: Int): IItemStack?
    operator fun set(index: Int, stack: IItemStack)

    override fun iterator(): Iterator<IItemStack> = (0..<size)
        .asSequence()
        .map { requireNotNull(get(it)) }
        .iterator()

    override fun isEmpty(): Boolean = size <= 0
    override fun contains(element: IItemStack): Boolean = iterator().asSequence().any { it == element }
    override fun containsAll(elements: Collection<IItemStack>): Boolean = elements.all { contains(it) }
}

interface IInventoryFactory {
    fun create(inventory: Inventory): IInventory
}
