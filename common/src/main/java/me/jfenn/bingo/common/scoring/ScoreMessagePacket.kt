package me.jfenn.bingo.common.scoring

import me.jfenn.bingo.api.IPacketBuf
import me.jfenn.bingo.api.item.IItemStack
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.event.packet.PacketConverter
import me.jfenn.bingo.common.team.BingoPlayerProfile
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.util.*

data class ScoreMessagePacket(
    val player: BingoPlayerProfile?,
    val isViewerOnTeam: Boolean,
    val item: IItemStack?,
    val image: String? = null,
    val messageType: MessageType?,
    val message: Text,
) {
    enum class MessageType {
        LEADING_TEAM,
        LINE_SCORED,
        ITEM_SCORED,
        CARD_COMPLETED,
    }

    object V1 : PacketConverter<ScoreMessagePacket> {
        override val id: Identifier = Identifier.of(MOD_ID_BINGO, "score_message")!!

        override fun toPacketBuf(source: ScoreMessagePacket, dest: IPacketBuf) {
            if (source.player != null) {
                dest.writeBoolean(true)
                dest.writeString(source.player.uuid.toString())
                dest.writeString(source.player.name)
            } else {
                dest.writeBoolean(false)
            }

            dest.writeBoolean(source.isViewerOnTeam)

            if (source.item != null) {
                dest.writeBoolean(true)
                dest.writeItemStack(source.item)
            } else {
                dest.writeBoolean(false)
            }

            dest.writeString(source.messageType?.name ?: "")
            dest.writeText(source.message)
        }

        override fun fromPacketBuf(buf: IPacketBuf): ScoreMessagePacket {
            return ScoreMessagePacket(
                player = if (buf.readBoolean()) {
                    BingoPlayerProfile(
                        uuid = UUID.fromString(buf.readString()),
                        name = buf.readString(),
                    )
                } else null,
                isViewerOnTeam = buf.readBoolean(),
                item = if (buf.readBoolean()) buf.readItemStack() else null,
                messageType = try {
                    MessageType.valueOf(buf.readString())
                } catch (e: Throwable) {
                    null
                },
                message = buf.readText(),
            )
        }
    }

    object V2 : PacketConverter<ScoreMessagePacket> {
        override val id: Identifier = Identifier.of(MOD_ID_BINGO, "score_message_v2")!!

        override fun toPacketBuf(source: ScoreMessagePacket, dest: IPacketBuf) {
            dest.writeNullable(source.player) {
                dest.writeString(it.uuid.toString())
                dest.writeString(it.name)
            }

            dest.writeBoolean(source.isViewerOnTeam)

            dest.writeNullable(source.item, dest::writeItemStack)
            dest.writeNullable(source.image, dest::writeString)

            dest.writeString(source.messageType?.name ?: "")
            dest.writeText(source.message)
        }

        override fun fromPacketBuf(buf: IPacketBuf): ScoreMessagePacket {
            return ScoreMessagePacket(
                player = buf.readNullable {
                    BingoPlayerProfile(
                        uuid = UUID.fromString(buf.readString()),
                        name = buf.readString(),
                    )
                },
                isViewerOnTeam = buf.readBoolean(),
                item = buf.readNullable(buf::readItemStack),
                image = buf.readNullable(buf::readString),
                messageType = try {
                    MessageType.valueOf(buf.readString())
                } catch (e: Throwable) {
                    null
                },
                message = buf.readText(),
            )
        }
    }
}