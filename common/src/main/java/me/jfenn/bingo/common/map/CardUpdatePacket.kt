package me.jfenn.bingo.common.map

import me.jfenn.bingo.api.IPacketBuf
import me.jfenn.bingo.common.card.BingoTeamKey
import me.jfenn.bingo.common.event.packet.PacketConverter
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

class CardUpdatePacket(
    val view: CardView,
) {

    object V1 : PacketConverter<CardUpdatePacket> {
        override val id: Identifier = Identifier("bingo", "update_card")

        override fun fromPacketBuf(buf: IPacketBuf): CardUpdatePacket {
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

        override fun toPacketBuf(source: CardUpdatePacket, dest: IPacketBuf) {
            val view = source.view

            if (view.teamKey != null) {
                dest.writeBoolean(true)
                dest.writeString(view.teamKey.id)
            } else {
                dest.writeBoolean(false)
            }

            if (view.teamColor != null) {
                dest.writeBoolean(true)
                dest.writeString(view.teamColor.name)
            } else {
                dest.writeBoolean(false)
            }

            dest.writeString(view.teamName)

            // read-only copy of tiles to avoid concurrent modification
            val tiles = view.tiles.toList()
            dest.writeInt(tiles.size)
            for (tile in tiles) {
                tile.toPacketBuf(dest)
            }
        }

    }

}