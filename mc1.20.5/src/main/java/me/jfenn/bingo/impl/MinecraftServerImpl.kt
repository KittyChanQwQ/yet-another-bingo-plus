package me.jfenn.bingo.impl

import me.jfenn.bingo.api.IMapService
import me.jfenn.bingo.api.ITickManager
import net.minecraft.component.type.MapIdComponent
import net.minecraft.item.map.MapState
import net.minecraft.server.MinecraftServer

class MinecraftServerImpl(
    private val server: MinecraftServer,
): IMapService, ITickManager {
    override fun getNextMapId(): Int {
        return server.overworld.nextMapId.id
    }

    override fun putMapState(id: Int, state: MapState) {
        server.overworld.putMapState(MapIdComponent(id), state)
    }

    override fun setFrozen(frozen: Boolean) {
        server.tickManager.isFrozen = frozen
    }
}