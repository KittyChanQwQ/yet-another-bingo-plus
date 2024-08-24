package me.jfenn.bingo.common.event.model

import me.jfenn.bingo.api.IPlayerHandle
import net.minecraft.server.MinecraftServer

open class PlayerEvent(
    val server: MinecraftServer,
    val player: IPlayerHandle
)
