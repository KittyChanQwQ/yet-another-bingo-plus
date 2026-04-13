package me.jfenn.bingo.platform

import me.jfenn.bingo.platform.resource.IResourceManager
import net.minecraft.server.MinecraftServer

interface IMinecraftServer {
    val server: MinecraftServer
    val ticks: Int
    val isDedicated: Boolean
    val isSingleplayer: Boolean
    var messageOfTheDay: String
    val currentPlayerCount: Int
    val maxPlayerCount: Int
    val resourceManager: IResourceManager
    fun stop(shutdownEventLoop: Boolean)
}