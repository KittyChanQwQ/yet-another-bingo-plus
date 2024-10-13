package me.jfenn.bingo.common.event.model

import me.jfenn.bingo.api.IPlayerHandle
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Hand
import net.minecraft.world.World

class UseItemEvent(
    val server: MinecraftServer,
    val player: IPlayerHandle,
    val world: World,
    val hand: Hand,
)
