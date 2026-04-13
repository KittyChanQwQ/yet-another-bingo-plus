package me.jfenn.bingo.common.map

import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.utils.text.TextColor
import me.jfenn.bingo.platform.IPacketBuf
import me.jfenn.bingo.platform.Identifier
import me.jfenn.bingo.platform.packet.PacketConverter
import me.jfenn.bingo.platform.text.Formatting

data class CardDisplayPacket(
    val display: Map<BingoTeamKey?, CardDisplay>,
) {
    object V1 : PacketConverter<CardDisplayPacket> {
        override val id = Identifier.of(MOD_ID_BINGO, "display_cards")

        override fun toPacketBuf(source: CardDisplayPacket, dest: IPacketBuf) {
            dest.writeList(source.display.entries) {
                dest.writeNullable(it.key?.id, dest::writeString)
                CardDisplay.V1.toPacketBuf(it.value, dest)
            }
        }

        override fun fromPacketBuf(buf: IPacketBuf): CardDisplayPacket {
            val display = buf.readList {
                val key = buf.readNullable(buf::readString)?.let { BingoTeamKey(it) }
                val display = CardDisplay.V1.fromPacketBuf(buf)
                Pair(key, display)
            }

            return CardDisplayPacket(
                display = display.toMap()
            )
        }
    }
}