package me.jfenn.bingo.common.timer

import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.event.packet.PacketConverter
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier

class CountdownPacket(
    val secondsRemaining: Int,
) {
    object V1 : PacketConverter<CountdownPacket> {
        override val id = Identifier(MOD_ID_BINGO, "countdown")

        override fun fromPacketBuf(buf: PacketByteBuf): CountdownPacket {
            return CountdownPacket(buf.readInt())
        }

        override fun toPacketBuf(source: CountdownPacket): PacketByteBuf {
            val buf = PacketByteBufs.create()
            buf.writeInt(source.secondsRemaining)
            return buf
        }
    }
}