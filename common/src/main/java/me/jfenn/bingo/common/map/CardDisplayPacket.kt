package me.jfenn.bingo.common.map

import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.utils.text.TextColor
import me.jfenn.bingo.platform.IPacketBuf
import me.jfenn.bingo.platform.packet.PacketConverter
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

data class CardDisplayPacket(
    val teamInfo: Map<BingoTeamKey, Formatting>,
    val display: List<BingoTeamKey?>,
) {

    object V1 : PacketConverter<CardDisplayPacket> {
        override val id = Identifier.of("bingo", "display_cards")!!

        override fun toPacketBuf(source: CardDisplayPacket, dest: IPacketBuf) {
            dest.writeInt(source.teamInfo.size)
            for ((key, value) in source.teamInfo) {
                dest.writeString(key.id)
                dest.writeString(
                    try {
                        // Previous versions use TextColor instead of Formatting, and crash if invalid values are received
                        TextColor.valueOf(value.name).name
                    } catch (e: IllegalArgumentException) {
                        TextColor.WHITE.name
                    }
                )
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
                BingoTeamKey(buf.readString()) to try {
                    Formatting.valueOf(buf.readString())
                } catch (e: IllegalArgumentException) {
                    Formatting.WHITE
                }
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