package me.jfenn.bingo.common.map

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import me.jfenn.bingo.common.card.BingoTeam
import net.minecraft.item.map.MapState

@Serializable
data class BingoMap(
    val mapId: Int,
    val team: BingoTeam?,
    var updatedAt: Instant = Instant.fromEpochMilliseconds(0L),
) {

    @Transient
    val teamColor = team?.color ?: 22

    @Transient
    var mapState: MapState? = null

}
