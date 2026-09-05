package me.jfenn.bingo.impl

import me.jfenn.bingo.platform.IMapColorService
import net.minecraft.world.level.material.MapColor

class MapColorServiceImpl : IMapColorService {
    override fun getMapColors(): Map<Byte, Int> {
        return  (4 until 248)
            .associate { i ->
                val mapColorRGB = MapColor.getColorFromPackedId(i)
                i.toUByte().toByte() to mapColorRGB
            }
    }
}