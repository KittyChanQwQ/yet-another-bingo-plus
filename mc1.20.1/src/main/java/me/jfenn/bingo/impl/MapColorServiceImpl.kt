package me.jfenn.bingo.impl

import me.jfenn.bingo.api.IMapColorService
import me.jfenn.bingo.common.map.Color
import me.jfenn.bingo.common.map.rgb
import net.minecraft.block.MapColor

class MapColorServiceImpl : IMapColorService {
    override fun getMapColors(): Map<Byte, Int> {
        return  (4 until 248)
            .associate { i ->
                val mapColorRGB = MapColor.getRenderColor(i)
                val (b, g, r) = Color.fromInt(mapColorRGB)
                i.toUByte().toByte() to rgb(r, g, b)
            }
    }
}