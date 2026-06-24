package me.jfenn.bingo.impl

import me.jfenn.bingo.impl.resource.ResourceManagerImpl
import me.jfenn.bingo.platform.IMinecraftServer
import me.jfenn.bingo.platform.ITickManager
import me.jfenn.bingo.platform.resource.IResourceManager
import net.minecraft.server.MinecraftServer

class MinecraftServerImpl(
    override val server: MinecraftServer,
): ITickManager, IMinecraftServer {
    override val isSingleplayer: Boolean
        get() = server.isSingleplayer

    override val isDedicated: Boolean
        get() = server.isDedicatedServer

    override val ticks: Int
        get() = server.tickCount

    override var messageOfTheDay: String by server::motd

    override val currentPlayerCount: Int
        get() = server.playerCount
    override val maxPlayerCount: Int
        get() = server.maxPlayers

    override fun setFrozen(frozen: Boolean) {
        server.tickRateManager().isFrozen = frozen
    }

    override val resourceManager: IResourceManager
        get() = ResourceManagerImpl(server.resourceManager)

    override fun saveAll() {
        server.saveEverything(true, false, true)
    }

    override fun stop(shutdownEventLoop: Boolean) {
        server.halt(shutdownEventLoop)
    }
}