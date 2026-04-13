package me.jfenn.bingo.platform.world

import me.jfenn.bingo.platform.inventory.IInventory

interface IBlockEntity

interface IChestBlockEntity : IBlockEntity {
    val inventory: IInventory
}