package me.jfenn.bingo.common.event.model

import net.minecraft.entity.Entity
import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ServerWorld

data class EntityLoadEvent(
    val server: MinecraftServer,
    val entity: Entity,
    val world: ServerWorld
)