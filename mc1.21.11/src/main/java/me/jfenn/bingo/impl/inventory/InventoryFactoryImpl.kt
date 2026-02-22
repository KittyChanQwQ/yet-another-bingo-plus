package me.jfenn.bingo.impl.inventory

import me.jfenn.bingo.platform.inventory.IInventory
import me.jfenn.bingo.platform.inventory.IInventoryFactory
import me.jfenn.bingo.platform.item.IItemStackFactory
import net.minecraft.inventory.Inventory

class InventoryFactoryImpl(
    private val itemStackFactory: IItemStackFactory,
) : IInventoryFactory {
    override fun create(inventory: Inventory): IInventory {
        return InventoryImpl(itemStackFactory, inventory)
    }
}
