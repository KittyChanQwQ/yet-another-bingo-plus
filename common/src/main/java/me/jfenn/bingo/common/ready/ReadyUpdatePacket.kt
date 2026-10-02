package me.jfenn.bingo.common.ready

import me.jfenn.bingo.api.data.BingoGameStatus
import me.jfenn.bingo.api.state.GameState
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.platform.IPacketBuf
import me.jfenn.bingo.platform.Identifier
import me.jfenn.bingo.platform.packet.PacketConverter
import me.jfenn.bingo.platform.readEnum
import me.jfenn.bingo.platform.text.IText
import java.time.Duration
import java.util.*

data class ReadyUpdatePacket(
    val isRunning: Boolean,
    val isReady: Boolean,
    val state: GameState,
    val remainingDuration: Duration,
    val totalDuration: Duration,
    val readyPlayers: Int,
    val totalPlayers: Int,
    val title: IText?,
    val subtitle: IText?,
    val canSendReady: Boolean = false,
    val voteOptions: List<VoteOption> = emptyList(),
    val votePending: UUID? = null,
) {
    object V2 : PacketConverter<ReadyUpdatePacket> {
        override val id: Identifier = Identifier.of(MOD_ID_BINGO, "ready_timer_update_v2")

        override fun fromPacketBuf(buf: IPacketBuf): ReadyUpdatePacket {
            return ReadyUpdatePacket(
                isRunning = buf.readBoolean(),
                isReady = buf.readBoolean(),
                state = GameState(buf.readString(), buf.readEnum()),
                remainingDuration = buf.readDuration(),
                totalDuration = buf.readDuration(),
                readyPlayers = buf.readInt(),
                totalPlayers = buf.readInt(),
                title = buf.readNullable(buf::readText),
                subtitle = buf.readNullable(buf::readText),
                canSendReady = buf.readBoolean(),
                voteOptions = buf.readList { VoteOption.V1.fromPacketBuf(buf) },
                votePending = buf.readNullable(buf::readUUID),
            )
        }

        override fun toPacketBuf(source: ReadyUpdatePacket, dest: IPacketBuf) {
            dest.writeBoolean(source.isRunning)
            dest.writeBoolean(source.isReady)
            dest.writeString(source.state.identifier)
            dest.writeEnum(source.state.status)
            dest.writeDuration(source.remainingDuration)
            dest.writeDuration(source.totalDuration)
            dest.writeInt(source.readyPlayers)
            dest.writeInt(source.totalPlayers)
            dest.writeNullable(source.title, dest::writeText)
            dest.writeNullable(source.subtitle, dest::writeText)
            dest.writeBoolean(source.canSendReady)
            dest.writeList(source.voteOptions) {
                VoteOption.V1.toPacketBuf(it, dest)
            }
            dest.writeNullable(source.votePending, dest::writeUUID)
        }
    }

    object V1 : PacketConverter<ReadyUpdatePacket> {
        override val id: Identifier = Identifier.of(MOD_ID_BINGO, "ready_timer_update")

        override fun fromPacketBuf(buf: IPacketBuf): ReadyUpdatePacket {
            return ReadyUpdatePacket(
                isRunning = buf.readBoolean(),
                isReady = buf.readBoolean(),
                state = GameState(buf.readString().lowercase(), BingoGameStatus.PREGAME),
                remainingDuration = buf.readDuration(),
                totalDuration = buf.readDuration(),
                readyPlayers = buf.readInt(),
                totalPlayers = buf.readInt(),
                title = buf.readNullable(buf::readText),
                subtitle = buf.readNullable(buf::readText),
                canSendReady = buf.readBoolean(),
            )
        }

        override fun toPacketBuf(source: ReadyUpdatePacket, dest: IPacketBuf) {
            dest.writeBoolean(source.isRunning)
            dest.writeBoolean(source.isReady)
            dest.writeString(source.state.identifier.uppercase())
            dest.writeDuration(source.remainingDuration)
            dest.writeDuration(source.totalDuration)
            dest.writeInt(source.readyPlayers)
            dest.writeInt(source.totalPlayers)
            dest.writeNullable(source.title, dest::writeText)
            dest.writeNullable(source.subtitle, dest::writeText)
            dest.writeBoolean(source.canSendReady)
        }
    }
}