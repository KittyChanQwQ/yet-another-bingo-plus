package me.jfenn.bingo.event

import me.jfenn.bingo.utils.EventListener
import net.minecraft.server.network.ServerPlayerEntity

object PlayerEvents {
    val onPlayerJoin = EventListener<ServerPlayerEntity>()
}