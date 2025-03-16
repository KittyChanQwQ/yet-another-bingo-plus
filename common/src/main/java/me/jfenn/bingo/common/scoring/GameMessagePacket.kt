package me.jfenn.bingo.common.scoring

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jfenn.bingo.client.platform.text.ITextSerialized
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.map.CardTile
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.platform.IPacketBuf
import me.jfenn.bingo.platform.item.IItemStackSerialized
import me.jfenn.bingo.platform.packet.PacketConverter
import me.jfenn.bingo.platform.readEnum
import me.jfenn.bingo.platform.utils.UuidAsString
import net.minecraft.util.Identifier
import java.util.*

data class GameMessagePacket(
    val id: UuidAsString = UUID.randomUUID(),
    val createdAt: Instant = Clock.System.now(),
    val team: BingoTeamKey,
    val icon: MessageIcon?,
    val decoration: CardTile.Decoration?,
    val messageType: ScoreMessagePacket.MessageType,
    val message: ITextSerialized,
    val isUpdate: Boolean = false,
) {
    sealed interface MessageIcon {
        class Item(
            val item: IItemStackSerialized,
            val itemList: List<IItemStackSerialized>,
        ) : MessageIcon
        class Texture(
            val texture: String,
        ) : MessageIcon
    }

    object V1 : PacketConverter<GameMessagePacket> {
        override val id: Identifier = Identifier.of(MOD_ID_BINGO, "game_message")!!

        override fun toPacketBuf(source: GameMessagePacket, dest: IPacketBuf) {
            dest.writeUUID(source.id)
            dest.writeInstant(source.createdAt)
            dest.writeString(source.team.id)

            when (val icon = source.icon) {
                is MessageIcon.Item -> {
                    dest.writeInt(0)
                    dest.writeItemStack(icon.item)
                    dest.writeList(icon.itemList, dest::writeItemStack)
                }
                is MessageIcon.Texture -> {
                    dest.writeInt(1)
                    dest.writeString(icon.texture)
                }
                else -> dest.writeInt(-1)
            }
            dest.writeNullable(source.decoration, dest::writeEnum)

            dest.writeEnum(source.messageType)
            dest.writeText(source.message)
            dest.writeBoolean(source.isUpdate)
        }

        override fun fromPacketBuf(buf: IPacketBuf): GameMessagePacket {
            val id = buf.readUUID()
            val createdAt = buf.readInstant()
            val team = BingoTeamKey(buf.readString())

            val icon = when (buf.readInt()) {
                0 -> MessageIcon.Item(
                    item = buf.readItemStack(),
                    itemList = buf.readList(buf::readItemStack),
                )
                1 -> MessageIcon.Texture(
                    texture = buf.readString(),
                )
                else -> null
            }
            val decoration = buf.readNullable { buf.readEnum<CardTile.Decoration>() }

            val messageType = buf.readEnum<ScoreMessagePacket.MessageType>()
            val message = buf.readText()

            val isUpdate = buf.readBoolean()

            return GameMessagePacket(
                id = id,
                createdAt = createdAt,
                team = team,
                icon = icon,
                decoration = decoration,
                messageType = messageType,
                message = message,
                isUpdate = isUpdate,
            )
        }
    }
}