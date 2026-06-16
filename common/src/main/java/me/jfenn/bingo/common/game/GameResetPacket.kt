package me.jfenn.bingo.common.game

import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.platform.IPacketBuf
import me.jfenn.bingo.platform.Identifier
import me.jfenn.bingo.platform.packet.PacketConverter

class GameResetPacket {
    object V1 : PacketConverter<GameResetPacket> {
        override val id: Identifier = Identifier.of(MOD_ID_BINGO, "game_reset")

        override fun fromPacketBuf(buf: IPacketBuf): GameResetPacket {
            return GameResetPacket()
        }

        override fun toPacketBuf(source: GameResetPacket, dest: IPacketBuf) {}
    }
}