package me.jfenn.bingo.api.item

import net.minecraft.server.network.ServerPlayerEntity

interface IPlayerHead : IItemStack {
    fun setSkullOwner(player: ServerPlayerEntity)
}