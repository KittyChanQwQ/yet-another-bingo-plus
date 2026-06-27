package me.jfenn.bingo.common.map

import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.platform.IPacketBuf
import me.jfenn.bingo.platform.Identifier
import me.jfenn.bingo.platform.packet.PacketConverter

class CardOpenPacket(
    val teamKey: BingoTeamKey?
) {
    object V1 : PacketConverter<CardOpenPacket> {
        override val id: Identifier = Identifier.of(MOD_ID_BINGO, "open_card")

        override fun toPacketBuf(source: CardOpenPacket, dest: IPacketBuf) {
            dest.writeNullable(source.teamKey?.id, dest::writeString)
        }

        override fun fromPacketBuf(buf: IPacketBuf): CardOpenPacket {
            return CardOpenPacket(
                teamKey = buf.readNullable { BingoTeamKey(buf.readString()) }
            )
        }
    }
}
