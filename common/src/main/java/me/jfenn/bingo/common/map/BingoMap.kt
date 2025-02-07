package me.jfenn.bingo.common.map

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.item.map.MapState

@Serializable
data class BingoMap(
    val mapId: Int,
) {

    @Transient
    var view: CardView? = null

    @Transient
    var mapState: MapState? = null

}
