package me.jfenn.bingo.common.ready

import me.jfenn.bingo.api.IPacketBuf
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.event.packet.PacketConverter
import me.jfenn.bingo.common.state.GameState
import net.minecraft.util.Identifier
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class ReadyUpdatePacket(
    val isRunning: Boolean,
    val isReady: Boolean,
    val state: GameState,
    val remainingDuration: Duration,
    val totalDuration: Duration,
    val readyPlayers: Int,
    val totalPlayers: Int,
) {
    object V1 : PacketConverter<ReadyUpdatePacket> {
        override val id: Identifier = Identifier.of(MOD_ID_BINGO, "ready_timer_update")!!

        override fun fromPacketBuf(buf: IPacketBuf): ReadyUpdatePacket {
            return ReadyUpdatePacket(
                isRunning = buf.readBoolean(),
                isReady = buf.readBoolean(),
                state = try {
                    GameState.valueOf(buf.readString())
                } catch (e: IllegalArgumentException) {
                    GameState.PREGAME
                },
                remainingDuration = buf.readLong().milliseconds,
                totalDuration = buf.readLong().milliseconds,
                readyPlayers = buf.readInt(),
                totalPlayers = buf.readInt(),
            )
        }

        override fun toPacketBuf(source: ReadyUpdatePacket, dest: IPacketBuf) {
            dest.writeBoolean(source.isRunning)
            dest.writeBoolean(source.isReady)
            dest.writeString(source.state.name)
            dest.writeLong(source.remainingDuration.inWholeMilliseconds)
            dest.writeLong(source.totalDuration.inWholeMilliseconds)
            dest.writeInt(source.readyPlayers)
            dest.writeInt(source.totalPlayers)
        }
    }
}