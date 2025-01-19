package me.jfenn.bingo.common.map

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.item.map.MapState

@Serializable
data class BingoMap(
    val mapId: Int,
) {

    @Transient
    var view: CardView = CardView(null, CardDisplay(null), mutableListOf())

    @Transient
    var mapState: MapState? = null

}
