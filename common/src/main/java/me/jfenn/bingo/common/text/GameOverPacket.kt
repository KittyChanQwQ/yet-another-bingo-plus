package me.jfenn.bingo.common.text

import me.jfenn.bingo.common.card.BingoTeamKey
import me.jfenn.bingo.common.event.packet.PacketConverter
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class GameOverPacket(
    val title: Text,
    val subtitle: Text,
    val winner: BingoTeamKey?,
    val duration: Duration,
    val canSendRestart: Boolean,
    val isWinner: Boolean,
) {
    object V1 : PacketConverter<GameOverPacket> {
        override val id = Identifier("bingo", "game_over")

        override fun fromPacketBuf(buf: PacketByteBuf): GameOverPacket {
            return GameOverPacket(
                title = buf.readText(),
                subtitle = buf.readText(),
                winner = if (buf.readBoolean()) BingoTeamKey(buf.readString()) else null,
                duration = buf.readLong().milliseconds,
                canSendRestart = buf.readBoolean(),
                isWinner = false,
            )
        }

        override fun toPacketBuf(source: GameOverPacket): PacketByteBuf {
            val buf = PacketByteBufs.create()
            buf.writeText(source.title)
            buf.writeText(source.subtitle)

            if (source.winner != null) {
                buf.writeBoolean(true)
                buf.writeString(source.winner.id)
            } else {
                buf.writeBoolean(false)
            }

            buf.writeLong(source.duration.inWholeMilliseconds)
            buf.writeBoolean(source.canSendRestart)
            return buf
        }
    }

    object V2 : PacketConverter<GameOverPacket> {
        override val id = Identifier("bingo", "game_over_v2")

        override fun fromPacketBuf(buf: PacketByteBuf): GameOverPacket {
            return GameOverPacket(
                title = buf.readText(),
                subtitle = buf.readText(),
                winner = if (buf.readBoolean()) BingoTeamKey(buf.readString()) else null,
                duration = buf.readLong().milliseconds,
                canSendRestart = buf.readBoolean(),
                isWinner = buf.readBoolean(),
            )
        }

        override fun toPacketBuf(source: GameOverPacket): PacketByteBuf {
            val buf = PacketByteBufs.create()
            buf.writeText(source.title)
            buf.writeText(source.subtitle)

            if (source.winner != null) {
                buf.writeBoolean(true)
                buf.writeString(source.winner.id)
            } else {
                buf.writeBoolean(false)
            }

            buf.writeLong(source.duration.inWholeMilliseconds)
            buf.writeBoolean(source.canSendRestart)
            buf.writeBoolean(source.isWinner)
            return buf
        }
    }
}