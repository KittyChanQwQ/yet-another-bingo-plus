package me.jfenn.bingo.common.text

import me.jfenn.bingo.common.card.BingoTeamKey
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.Text
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class GameOverPacket(
    val title: Text,
    val subtitle: Text,
    val winner: BingoTeamKey?,
    val duration: Duration,
    val canSendRestart: Boolean,
) {

    fun toPacketBuf(): PacketByteBuf {
        val buf = PacketByteBufs.create()
        buf.writeText(title)
        buf.writeText(subtitle)

        if (winner != null) {
            buf.writeBoolean(true)
            buf.writeString(winner.id)
        } else {
            buf.writeBoolean(false)
        }

        buf.writeLong(duration.inWholeMilliseconds)
        buf.writeBoolean(canSendRestart)
        return buf
    }

    companion object {
        fun fromPacketBuf(buf: PacketByteBuf): GameOverPacket {
            return GameOverPacket(
                title = buf.readText(),
                subtitle = buf.readText(),
                winner = if (buf.readBoolean()) BingoTeamKey(buf.readString()) else null,
                duration = buf.readLong().milliseconds,
                canSendRestart = buf.readBoolean(),
            )
        }
    }

}