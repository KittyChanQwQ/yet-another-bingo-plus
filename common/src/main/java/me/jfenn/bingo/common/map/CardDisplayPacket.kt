package me.jfenn.bingo.common.map

import me.jfenn.bingo.common.card.BingoTeamKey
import me.jfenn.bingo.common.event.packet.PacketConverter
import me.jfenn.bingo.common.utils.text.TextColor
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier

data class CardDisplayPacket(
    val teamInfo: Map<BingoTeamKey, TextColor>,
    val display: List<BingoTeamKey?>,
) {

    object V1 : PacketConverter<CardDisplayPacket> {
        override val id = Identifier("bingo", "display_cards")

        override fun toPacketBuf(source: CardDisplayPacket): PacketByteBuf {
            val buf = PacketByteBufs.create()

            buf.writeInt(source.teamInfo.size)
            for ((key, value) in source.teamInfo) {
                buf.writeString(key.id)
                buf.writeString(value.name)
            }

            buf.writeInt(source.display.size)
            for (key in source.display) {
                if (key != null) {
                    buf.writeBoolean(true)
                    buf.writeString(key.id)
                } else {
                    buf.writeBoolean(false)
                }
            }

            return buf
        }

        override fun fromPacketBuf(buf: PacketByteBuf): CardDisplayPacket {
            val teamInfo = List(buf.readInt()) {
                BingoTeamKey(buf.readString()) to TextColor.valueOf(buf.readString())
            }.toMap()

            val display = List(buf.readInt()) {
                if (buf.readBoolean())
                    BingoTeamKey(buf.readString())
                else null
            }

            return CardDisplayPacket(
                teamInfo = teamInfo,
                display = display,
            )
        }
    }

}