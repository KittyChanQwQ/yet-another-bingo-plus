package me.jfenn.bingo.platform

import net.minecraft.item.map.MapState

interface IMapService {
    fun getNextMapId(): Int
    fun putMapState(id: Int, state: MapState)
}