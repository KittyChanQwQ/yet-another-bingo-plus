package me.jfenn.bingo.common.map

import me.jfenn.bingo.api.IPacketBuf
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.event.packet.PacketConverter
import me.jfenn.bingo.common.utils.text.TextColor
import net.minecraft.util.Identifier

data class CardDisplayPacket(
    val teamInfo: Map<BingoTeamKey, TextColor>,
    val display: List<BingoTeamKey?>,
) {

    object V1 : PacketConverter<CardDisplayPacket> {
        override val id = Identifier.of("bingo", "display_cards")!!

        override fun toPacketBuf(source: CardDisplayPacket, dest: IPacketBuf) {
            dest.writeInt(source.teamInfo.size)
            for ((key, value) in source.teamInfo) {
                dest.writeString(key.id)
                dest.writeString(value.name)
            }

            dest.writeInt(source.display.size)
            for (key in source.display) {
                if (key != null) {
                    dest.writeBoolean(true)
                    dest.writeString(key.id)
                } else {
                    dest.writeBoolean(false)
                }
            }
        }

        override fun fromPacketBuf(buf: IPacketBuf): CardDisplayPacket {
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