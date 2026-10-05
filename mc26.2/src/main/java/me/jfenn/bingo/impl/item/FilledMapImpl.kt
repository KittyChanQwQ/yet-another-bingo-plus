package me.jfenn.bingo.impl.item

import me.jfenn.bingo.platform.item.IFilledMap
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.MapItemColor
import net.minecraft.world.level.saveddata.maps.MapId

class FilledMapImpl(
    override val stack: ItemStack,
) : ItemStackImpl(stack), IFilledMap {
    override var mapId: Int?
        get() = stack[DataComponents.MAP_ID]?.id
        set(value) {
            stack[DataComponents.MAP_ID] = value?.let { MapId(it) }
        }

    override var mapColor: Int?
        get() = stack[DataComponents.MAP_COLOR]?.rgb
        set(value) {
            stack[DataComponents.MAP_COLOR] = value?.let { MapItemColor(it) }
        }
}