package me.jfenn.bingo.common.map

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.item.map.MapState

@Serializable
data class BingoMap(
    val mapId: Int,
    val tiles: MutableList<BingoMapTile> = MutableList(25) { BingoMapTile() },
) {

    @Transient
    var updatedAt: Instant = Instant.fromEpochMilliseconds(0L)

    @Transient
    var mapState: MapState? = null

    fun tile(x: Int, y: Int): BingoMapTile {
        assert(x in 0 until 5) { "x=$x is not within the required 0..4 range" }
        assert(y in 0 until 5) { "y=$y is not within the required 0..4 range" }

        return tiles[x + y * 5]
    }

    fun setTile(x: Int, y: Int, tile: BingoMapTile) {
        assert(x in 0 until 5) { "x=$x is not within the required 0..4 range" }
        assert(y in 0 until 5) { "y=$y is not within the required 0..4 range" }

        tiles[x + y * 5] = tile
    }

}
