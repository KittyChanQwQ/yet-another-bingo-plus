package me.jfenn.bingo.common.game

import me.jfenn.bingo.api.IPacketBuf
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.event.packet.PacketConverter
import me.jfenn.bingo.common.team.BingoTeamKey
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
    /**
     * Whether the packet is sent because of a game state change;
     * - false indicates that the client should open the game over screen
     * - true indicates that the client should update its data without opening the screen
     */
    val isUpdate: Boolean,
) {
    object V1 : PacketConverter<GameOverPacket> {
        override val id = Identifier.of("bingo", "game_over")!!

        override fun fromPacketBuf(buf: IPacketBuf): GameOverPacket {
            return GameOverPacket(
                title = buf.readText(),
                subtitle = buf.readText(),
                winner = if (buf.readBoolean()) BingoTeamKey(buf.readString()) else null,
                duration = buf.readLong().milliseconds,
                canSendRestart = buf.readBoolean(),
                isWinner = false,
                isUpdate = false,
            )
        }

        override fun toPacketBuf(source: GameOverPacket, dest: IPacketBuf) {
            dest.writeText(source.title)
            dest.writeText(source.subtitle)

            if (source.winner != null) {
                dest.writeBoolean(true)
                dest.writeString(source.winner.id)
            } else {
                dest.writeBoolean(false)
            }

            dest.writeLong(source.duration.inWholeMilliseconds)
            dest.writeBoolean(source.canSendRestart)
        }
    }

    object V2 : PacketConverter<GameOverPacket> {
        override val id = Identifier.of("bingo", "game_over_v2")!!

        override fun fromPacketBuf(buf: IPacketBuf): GameOverPacket {
            return GameOverPacket(
                title = buf.readText(),
                subtitle = buf.readText(),
                winner = if (buf.readBoolean()) BingoTeamKey(buf.readString()) else null,
                duration = buf.readLong().milliseconds,
                canSendRestart = buf.readBoolean(),
                isWinner = buf.readBoolean(),
                isUpdate = false,
            )
        }

        override fun toPacketBuf(source: GameOverPacket, dest: IPacketBuf) {
            dest.writeText(source.title)
            dest.writeText(source.subtitle)

            if (source.winner != null) {
                dest.writeBoolean(true)
                dest.writeString(source.winner.id)
            } else {
                dest.writeBoolean(false)
            }

            dest.writeLong(source.duration.inWholeMilliseconds)
            dest.writeBoolean(source.canSendRestart)
            dest.writeBoolean(source.isWinner)
        }
    }

    object V3 : PacketConverter<GameOverPacket> {
        override val id = Identifier.of(MOD_ID_BINGO, "game_over_v3")!!

        override fun fromPacketBuf(buf: IPacketBuf): GameOverPacket {
            return GameOverPacket(
                title = buf.readText(),
                subtitle = buf.readText(),
                winner = if (buf.readBoolean()) BingoTeamKey(buf.readString()) else null,
                duration = buf.readLong().milliseconds,
                canSendRestart = buf.readBoolean(),
                isWinner = buf.readBoolean(),
                isUpdate = buf.readBoolean(),
            )
        }

        override fun toPacketBuf(source: GameOverPacket, dest: IPacketBuf) {
            dest.writeText(source.title)
            dest.writeText(source.subtitle)

            if (source.winner != null) {
                dest.writeBoolean(true)
                dest.writeString(source.winner.id)
            } else {
                dest.writeBoolean(false)
            }

            dest.writeLong(source.duration.inWholeMilliseconds)
            dest.writeBoolean(source.canSendRestart)
            dest.writeBoolean(source.isWinner)
            dest.writeBoolean(source.isUpdate)
        }
    }
}