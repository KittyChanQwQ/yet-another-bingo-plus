package me.jfenn.bingo.impl

import me.jfenn.bingo.impl.resource.ResourceManagerImpl
import me.jfenn.bingo.platform.*
import me.jfenn.bingo.platform.resource.IResourceManager
import net.minecraft.server.MinecraftServer
import java.nio.file.Path

class MinecraftServerImpl(
    override val server: MinecraftServer,
): ITickManager, ILevelStorage, IMinecraftServer {
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

    override fun getLevelSaveDir(worldId: Identifier): Path? {
        val identifier = worldId.toMinecraftIdentifier()
        val world = server.allLevels.find { it.dimension().identifier() == identifier }
            ?: return null

        return server.accessor.storageSource.getDimensionPath(world.dimension())
    }

    override val resourceManager: IResourceManager
        get() = ResourceManagerImpl(server.resourceManager)

    override fun stop(shutdownEventLoop: Boolean) {
        server.halt(shutdownEventLoop)
    }
}