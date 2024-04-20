package me.jfenn.bingo.impl.item

import me.jfenn.bingo.api.item.IItemStack
import me.jfenn.bingo.api.item.IItemStackFactory
import me.jfenn.bingo.api.IMapService
import net.minecraft.item.FilledMapItem
import net.minecraft.item.Items

class MapServiceImpl(
    private val itemStackFactory: IItemStackFactory,
) : IMapService {
    override fun createFilledMap(mapId: Int, mapColor: Int?): IItemStack {
        return itemStackFactory.createStack(Items.FILLED_MAP).also {
            it.stack.getOrCreateNbt().apply {
                putInt("map", mapId)
            }
            if (mapColor != null) {
                it.stack.getOrCreateSubNbt("display").apply {
                    putInt("MapColor", mapColor)
                }
            }
        }
    }

    override fun isFilledMap(stack: IItemStack): Boolean {
        return stack.item == Items.FILLED_MAP
    }

    override fun getMapId(stack: IItemStack): Int? {
        return FilledMapItem.getMapId(stack.stack)
    }
}