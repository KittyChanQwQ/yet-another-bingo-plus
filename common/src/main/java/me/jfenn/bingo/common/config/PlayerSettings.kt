package me.jfenn.bingo.common.config

import kotlinx.serialization.Serializable
import me.jfenn.bingo.api.IPacketBuf
import me.jfenn.bingo.common.event.packet.PacketConverter
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

        override fun fromPacketBuf(buf: IPacketBuf): PlayerSettings {
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

        override fun toPacketBuf(source: PlayerSettings, dest: IPacketBuf) {
            dest.writeInt(0)
            dest.writeBoolean(source.seenTutorial)
            dest.writeBoolean(source.bossbar)
            dest.writeBoolean(source.scoreboard)
            dest.writeBoolean(source.scoreboardAutoHide)
            dest.writeBoolean(source.leadingMessages)
            dest.writeBoolean(source.scoreMessages)
            dest.writeBoolean(source.nightVision)
        }
    }

    object V1_C2S : PacketConverter<PlayerSettings> {
        override val id: Identifier = Identifier("bingo", "update_settings_c2s")
        override fun fromPacketBuf(buf: IPacketBuf) = V1_S2C.fromPacketBuf(buf)
        override fun toPacketBuf(source: PlayerSettings, dest: IPacketBuf) = V1_S2C.toPacketBuf(source, dest)
    }
}
