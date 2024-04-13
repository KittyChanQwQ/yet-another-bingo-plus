package me.jfenn.bingo.common.config

import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.event.packet.PacketConverter
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier

@Serializable
data class PlayerSettings(
    var seenTutorial: Boolean = false,
    var bossbar: Boolean = false,
    var scoreboard: Boolean = true,
    var scoreboardAutoHide: Boolean = false,
    var leadingMessages: Boolean = true,
    var scoreMessages: Boolean = true,
    var nightVision: Boolean = true,
) {
    object V1_S2C : PacketConverter<PlayerSettings> {
        override val id: Identifier = Identifier("bingo", "update_settings_s2c")

        override fun fromPacketBuf(buf: PacketByteBuf): PlayerSettings {
            buf.readInt()
            return PlayerSettings(
                seenTutorial = buf.readBoolean(),
                bossbar = buf.readBoolean(),
                scoreboard = buf.readBoolean(),
                scoreboardAutoHide = buf.readBoolean(),
                leadingMessages = buf.readBoolean(),
                scoreMessages = buf.readBoolean(),
                nightVision = buf.readBoolean(),
            )
        }

        override fun toPacketBuf(source: PlayerSettings): PacketByteBuf {
            val buf = PacketByteBufs.create()
            buf.writeInt(0)
            buf.writeBoolean(source.seenTutorial)
            buf.writeBoolean(source.bossbar)
            buf.writeBoolean(source.scoreboard)
            buf.writeBoolean(source.scoreboardAutoHide)
            buf.writeBoolean(source.leadingMessages)
            buf.writeBoolean(source.scoreMessages)
            buf.writeBoolean(source.nightVision)
            return buf
        }
    }

    object V1_C2S : PacketConverter<PlayerSettings> {
        override val id: Identifier = Identifier("bingo", "update_settings_c2s")
        override fun fromPacketBuf(buf: PacketByteBuf) = V1_S2C.fromPacketBuf(buf)
        override fun toPacketBuf(source: PlayerSettings) = V1_S2C.toPacketBuf(source)
    }
}
