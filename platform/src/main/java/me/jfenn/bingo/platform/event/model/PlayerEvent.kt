package me.jfenn.bingo.platform.event.model

import me.jfenn.bingo.platform.IPlayerHandle
import net.minecraft.server.MinecraftServer

open class PlayerEvent(
    val server: MinecraftServer,
    val player: IPlayerHandle
)
