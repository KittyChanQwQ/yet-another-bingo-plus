package me.jfenn.bingo.common.config

import kotlinx.serialization.Serializable
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.network.PacketByteBuf

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

    fun toPacketBuf(): PacketByteBuf {
        val buf = PacketByteBufs.create()
        buf.writeInt(VERSION)
        buf.writeBoolean(seenTutorial)
        buf.writeBoolean(bossbar)
        buf.writeBoolean(scoreboard)
        buf.writeBoolean(scoreboardAutoHide)
        buf.writeBoolean(leadingMessages)
        buf.writeBoolean(scoreMessages)
        buf.writeBoolean(nightVision)
        return buf
    }

    companion object {
        const val VERSION = 0

        fun fromPacketBuf(buf: PacketByteBuf): PlayerSettings? {
            val version = buf.readInt()
            if (version != VERSION) return null

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
    }

}
