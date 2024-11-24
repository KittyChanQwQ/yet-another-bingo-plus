package me.jfenn.bingo.impl

import me.jfenn.bingo.api.ILevelStorage
import me.jfenn.bingo.api.IMapService
import me.jfenn.bingo.api.ITickManager
import net.minecraft.component.type.MapIdComponent
import net.minecraft.item.map.MapState
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import java.nio.file.Path

class MinecraftServerImpl(
    private val server: MinecraftServer,
): IMapService, ITickManager, ILevelStorage {
    override fun getNextMapId(): Int {
        return server.overworld.increaseAndGetMapId().id
    }

    override fun putMapState(id: Int, state: MapState) {
        server.overworld.putMapState(MapIdComponent(id), state)
    }

    override fun setFrozen(frozen: Boolean) {
        server.tickManager.isFrozen = frozen
    }

    override fun getLevelSaveDir(worldId: Identifier): Path? {
        val world = server.worlds.find { it.registryKey.value == worldId }
            ?: return null

        return server.session.getWorldDirectory(world.registryKey)
    }
}