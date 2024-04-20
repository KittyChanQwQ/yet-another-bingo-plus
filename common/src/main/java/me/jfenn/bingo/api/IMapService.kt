package me.jfenn.bingo.api

interface IMapService {
    fun createFilledMap(mapId: Int, mapColor: Int?): IItemStack
    fun isFilledMap(stack: IItemStack): Boolean
    fun getMapId(stack: IItemStack): Int?
}