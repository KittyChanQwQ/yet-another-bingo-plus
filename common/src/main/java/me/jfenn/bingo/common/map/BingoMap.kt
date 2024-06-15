package me.jfenn.bingo.common.map

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.item.map.MapState
import net.minecraft.text.Text

@Serializable
data class BingoMap(
    val mapId: Int,
) {

    @Transient
    var view: CardView = CardView(null, null, Text.empty(), mutableListOf())

    @Transient
    var mapState: MapState? = null

}
