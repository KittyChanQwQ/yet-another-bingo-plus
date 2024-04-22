package me.jfenn.bingo.impl

import me.jfenn.bingo.api.IMapService
import me.jfenn.bingo.api.ITickManager
import net.minecraft.item.FilledMapItem
import net.minecraft.item.map.MapState
import net.minecraft.server.MinecraftServer

class MinecraftServerImpl(
    private val server: MinecraftServer,
): IMapService, ITickManager {
    override fun getNextMapId(): Int {
        return server.overworld.nextMapId
    }

    override fun putMapState(id: Int, state: MapState) {
        server.overworld.putMapState(FilledMapItem.getMapName(id), state)
    }

    override fun setFrozen(frozen: Boolean) = Unit
}