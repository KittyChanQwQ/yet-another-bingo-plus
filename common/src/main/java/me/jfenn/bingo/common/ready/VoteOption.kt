package me.jfenn.bingo.common.ready

import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.platform.IPacketBuf
import me.jfenn.bingo.platform.Identifier
import me.jfenn.bingo.platform.packet.PacketConverter
import me.jfenn.bingo.platform.text.IText
import java.util.*

data class VoteOption(
    val id: UUID,
    val title: IText,
    val votes: Int,
    val isSelected: Boolean,
) {
    object V1 : PacketConverter<VoteOption> {
        override val id = Identifier.of(MOD_ID_BINGO, "vote_option")

        override fun toPacketBuf(source: VoteOption, dest: IPacketBuf) {
            dest.writeUUID(source.id)
            dest.writeText(source.title)
            dest.writeInt(source.votes)
            dest.writeBoolean(source.isSelected)
        }

        override fun fromPacketBuf(buf: IPacketBuf): VoteOption {
            return VoteOption(
                id = buf.readUUID(),
                title = buf.readText(),
                votes = buf.readInt(),
                isSelected = buf.readBoolean(),
            )
        }
    }
}