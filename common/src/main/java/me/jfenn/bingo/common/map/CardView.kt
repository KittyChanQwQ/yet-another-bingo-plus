package me.jfenn.bingo.common.map

import me.jfenn.bingo.common.card.BingoTeamKey
import me.jfenn.bingo.common.utils.text.TextColor
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Formatting

data class CardView(
    val teamKey: BingoTeamKey?,
    val teamColor: Formatting?,
    val teamName: String,
    val tiles: MutableList<CardTile>,
) {

    fun tile(x: Int, y: Int): CardTile? {
        assert(x in 0 until 5) { "x=$x is not within the required 0..4 range" }
        assert(y in 0 until 5) { "y=$y is not within the required 0..4 range" }

        return tiles.getOrNull(x + y * 5)
    }

    fun toPacketBuf(): PacketByteBuf {
        val buf = PacketByteBufs.create()

        if (teamKey != null) {
            buf.writeBoolean(true)
            buf.writeString(teamKey.id)
        } else {
            buf.writeBoolean(false)
        }

        if (teamColor != null) {
            buf.writeBoolean(true)
            buf.writeString(teamColor.name)
        } else {
            buf.writeBoolean(false)
        }

        buf.writeString(teamName)

        buf.writeInt(tiles.size)
        for (tile in tiles) {
            tile.toPacketBuf(buf)
        }

        return buf
    }

    companion object {
        fun fromPacketBuf(buf: PacketByteBuf): CardView {
            val teamKey = if (buf.readBoolean()) BingoTeamKey(buf.readString()) else null
            val teamColor = if (buf.readBoolean()) Formatting.valueOf(buf.readString()) else null
            val teamName = buf.readString()
            val tilesLen = buf.readInt()
            val tiles = MutableList(tilesLen) { CardTile.fromPacketBuf(buf) }
            return CardView(
                teamKey = teamKey,
                teamColor = teamColor,
                teamName = teamName,
                tiles = tiles,
            )
        }
    }

}