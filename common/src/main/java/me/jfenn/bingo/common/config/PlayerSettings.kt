package me.jfenn.bingo.common.config

import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.utils.json
import me.jfenn.bingo.platform.IPacketBuf
import me.jfenn.bingo.platform.Identifier
import me.jfenn.bingo.platform.packet.PacketConverter

@Serializable
data class PlayerSettings(
    val seenTutorial: Boolean = false,
    val hideLobbyPrompt: Boolean = false,
    val bossbar: Boolean = false,
    val scoreboard: Boolean = true,
    val scoreboardAutoHide: Boolean = false,
    val leadingMessages: Boolean = true,
    val scoreMessages: Boolean = true,
    val itemMessages: Boolean = scoreMessages,
    val nightVision: Boolean = true,
) {
    object V2 : PacketConverter<PlayerSettings> {
        override val id: Identifier = Identifier.of(MOD_ID_BINGO, "update_settings")

        override fun fromPacketBuf(buf: IPacketBuf): PlayerSettings {
            val str = buf.readString()
            return json.decodeFromString(str)
        }

        override fun toPacketBuf(source: PlayerSettings, dest: IPacketBuf) {
            val str = json.encodeToString(source)
            dest.writeString(str)
        }
    }
}
