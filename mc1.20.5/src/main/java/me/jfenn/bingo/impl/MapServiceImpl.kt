package me.jfenn.bingo.impl

import me.jfenn.bingo.api.item.IItemStack
import me.jfenn.bingo.api.item.IItemStackFactory
import me.jfenn.bingo.api.IMapService
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.MapColorComponent
import net.minecraft.component.type.MapIdComponent
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

class MapServiceImpl(
    private val itemStackFactory: IItemStackFactory,
) : IMapService {
    override fun createFilledMap(mapId: Int, mapColor: Int?): IItemStack {
        return itemStackFactory.createStack(Items.FILLED_MAP).also {
            val stack: ItemStack = it.stack
            stack[DataComponentTypes.MAP_ID] = MapIdComponent(mapId)
            if (mapColor != null) {
                stack[DataComponentTypes.MAP_COLOR] = MapColorComponent(mapColor)
            }
        }
    }

    override fun isFilledMap(stack: IItemStack): Boolean {
        return stack.item == Items.FILLED_MAP
    }

    override fun getMapId(stack: IItemStack): Int? {
        val itemStack: ItemStack = stack.stack
        return itemStack[DataComponentTypes.MAP_ID]?.id
    }
}