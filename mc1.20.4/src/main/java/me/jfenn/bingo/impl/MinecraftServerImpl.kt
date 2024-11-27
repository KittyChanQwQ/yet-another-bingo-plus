package me.jfenn.bingo.impl

import me.jfenn.bingo.platform.ILevelStorage
import me.jfenn.bingo.platform.IMapService
import me.jfenn.bingo.platform.ITickManager
import net.minecraft.item.FilledMapItem
import net.minecraft.item.map.MapState
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import java.nio.file.Path

class MinecraftServerImpl(
    private val server: MinecraftServer,
): IMapService, ITickManager, me.jfenn.bingo.platform.ILevelStorage {
    override fun getNextMapId(): Int {
        return server.overworld.nextMapId
    }

    override fun putMapState(id: Int, state: MapState) {
        server.overworld.putMapState(FilledMapItem.getMapName(id), state)
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