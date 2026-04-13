package me.jfenn.bingo.platform.event.model

import me.jfenn.bingo.platform.Hand
import me.jfenn.bingo.platform.IEntity
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.IServerWorld
import me.jfenn.bingo.platform.event.IReturnEvent
import net.minecraft.server.MinecraftServer

class UseEntityEvent(
    val server: MinecraftServer,
    val player: IPlayerHandle,
    val world: IServerWorld,
    val hand: Hand,
    val entity: IEntity,
) {
    companion object : IReturnEvent<UseEntityEvent, ActionResult<Unit>?>
}
