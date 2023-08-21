package me.jfenn.bingo.event

import me.jfenn.bingo.utils.EventListener
import net.minecraft.server.network.ServerPlayerEntity

object PlayerEvents {
    // val beforePlayerJoin = EventListener<ServerPlayerEntity>()
    val onPlayerJoin = EventListener<ServerPlayerEntity>()
    val onPlayerLeave = EventListener<ServerPlayerEntity>()
}