package me.jfenn.bingo.common.map

import me.jfenn.bingo.common.card.BingoTeamKey
import me.jfenn.bingo.common.event.packet.PacketConverter
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

class CardUpdatePacket(
    val view: CardView,
) {

    object V1 : PacketConverter<CardUpdatePacket> {
        override val id: Identifier = Identifier("bingo", "update_card")

        override fun fromPacketBuf(buf: PacketByteBuf): CardUpdatePacket {
            val teamKey = if (buf.readBoolean()) BingoTeamKey(buf.readString()) else null
            val teamColor = if (buf.readBoolean()) Formatting.valueOf(buf.readString()) else null
            val teamName = buf.readString()
            val tilesLen = buf.readInt()
            val tiles = MutableList(tilesLen) { CardTile.fromPacketBuf(buf) }
            return CardUpdatePacket(
                CardView(
                    teamKey = teamKey,
                    teamColor = teamColor,
                    teamName = teamName,
                    tiles = tiles,
                )
            )
        }

        override fun toPacketBuf(source: CardUpdatePacket): PacketByteBuf {
            val view = source.view
            val buf = PacketByteBufs.create()

            if (view.teamKey != null) {
                buf.writeBoolean(true)
                buf.writeString(view.teamKey.id)
            } else {
                buf.writeBoolean(false)
            }

            if (view.teamColor != null) {
                buf.writeBoolean(true)
                buf.writeString(view.teamColor.name)
            } else {
                buf.writeBoolean(false)
            }

            buf.writeString(view.teamName)

            // read-only copy of tiles to avoid concurrent modification
            val tiles = view.tiles.toList()
            buf.writeInt(tiles.size)
            for (tile in tiles) {
                tile.toPacketBuf(buf)
            }

            return buf
        }

    }

}