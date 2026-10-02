package me.jfenn.bingo.common.ready

import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.platform.IPacketBuf
import me.jfenn.bingo.platform.Identifier
import me.jfenn.bingo.platform.packet.PacketConverter
import java.util.*

class SetReadyPacket(
    val isReady: Boolean,
    val optionId: UUID? = null,
) {
    object V2 : PacketConverter<SetReadyPacket> {
        override val id: Identifier = Identifier.of(MOD_ID_BINGO, "set_ready_v2")

        override fun toPacketBuf(source: SetReadyPacket, dest: IPacketBuf) {
            dest.writeBoolean(source.isReady)
            dest.writeNullable(source.optionId, dest::writeUUID)
        }

        override fun fromPacketBuf(buf: IPacketBuf): SetReadyPacket {
            return SetReadyPacket(
                isReady = buf.readBoolean(),
                optionId = buf.readNullable { buf.readUUID() },
            )
        }
    }

    object V1 : PacketConverter<SetReadyPacket> {
        override val id: Identifier = Identifier.of(MOD_ID_BINGO, "set_ready")

        override fun toPacketBuf(source: SetReadyPacket, dest: IPacketBuf) {
            dest.writeBoolean(source.isReady)
        }

        override fun fromPacketBuf(buf: IPacketBuf): SetReadyPacket {
            return SetReadyPacket(buf.readBoolean())
        }
    }
}