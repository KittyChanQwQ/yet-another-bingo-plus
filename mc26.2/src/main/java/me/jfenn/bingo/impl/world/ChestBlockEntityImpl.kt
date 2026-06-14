package me.jfenn.bingo.impl.world

import me.jfenn.bingo.impl.inventory.InventoryImpl
import me.jfenn.bingo.platform.inventory.IInventory
import me.jfenn.bingo.platform.item.IItemStackFactory
import me.jfenn.bingo.platform.world.IChestBlockEntity
import net.minecraft.world.level.block.entity.ChestBlockEntity

class ChestBlockEntityImpl(
    private val itemStackFactory: IItemStackFactory,
    private val entity: ChestBlockEntity,
) : IChestBlockEntity {
    override val inventory: IInventory
        get() = InventoryImpl(itemStackFactory, entity)
}
