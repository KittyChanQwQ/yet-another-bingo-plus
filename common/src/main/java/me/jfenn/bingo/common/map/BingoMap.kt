package me.jfenn.bingo.common.map

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.item.map.MapState

@Serializable
data class BingoMap(
    val mapId: Int,
) {

    @Transient
    var view: CardView = CardView(null, null, "", mutableListOf())

    @Transient
    var updatedAt: Instant = Instant.fromEpochMilliseconds(0L)

    @Transient
    var mapState: MapState? = null

}
