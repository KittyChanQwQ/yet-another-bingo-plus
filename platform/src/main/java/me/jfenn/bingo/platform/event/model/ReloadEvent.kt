package me.jfenn.bingo.platform.event.model

import net.minecraft.resource.ResourceManager
import net.minecraft.server.MinecraftServer

data class ReloadEvent(
    val server: MinecraftServer,
    val resourceManager: ResourceManager,
)
