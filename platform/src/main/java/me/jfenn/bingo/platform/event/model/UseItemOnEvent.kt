package me.jfenn.bingo.platform.event.model

import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.IServerWorld
import me.jfenn.bingo.platform.block.BlockPosition
import me.jfenn.bingo.platform.event.IEvent
import net.minecraft.server.MinecraftServer

class UseItemOnEvent(
    val server: MinecraftServer,
    val player: IPlayerHandle,
    val world: IServerWorld,
    val blockPos: BlockPosition,
) {
    companion object : IEvent<UseItemOnEvent>
}
