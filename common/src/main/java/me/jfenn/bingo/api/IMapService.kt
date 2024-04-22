package me.jfenn.bingo.api

import net.minecraft.item.map.MapState

interface IMapService {
    fun getNextMapId(): Int
    fun putMapState(id: Int, state: MapState)
}