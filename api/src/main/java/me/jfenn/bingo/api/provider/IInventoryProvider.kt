package me.jfenn.bingo.api.provider

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory

interface IInventoryProvider {
    fun getInventories(player: ServerPlayer) : List<Inventory>
}
