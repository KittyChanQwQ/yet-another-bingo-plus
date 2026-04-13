package me.jfenn.bingo.platform.event.model

import me.jfenn.bingo.platform.IEntity
import me.jfenn.bingo.platform.IServerWorld
import me.jfenn.bingo.platform.event.IEvent
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity

data class EntityLoadEvent(
    val server: MinecraftServer,
    val entity: IEntity,
    val world: IServerWorld
) {
    companion object : IEvent<EntityLoadEvent>
}