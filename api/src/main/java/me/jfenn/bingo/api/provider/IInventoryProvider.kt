package me.jfenn.bingo.api.provider

import net.minecraft.inventory.Inventory
import net.minecraft.server.network.ServerPlayerEntity

interface IInventoryProvider {
    fun getInventories(player: ServerPlayerEntity) : List<Inventory>
}
