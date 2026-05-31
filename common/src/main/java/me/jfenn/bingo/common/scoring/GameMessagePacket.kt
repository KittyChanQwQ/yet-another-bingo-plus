package me.jfenn.bingo.common.scoring

import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.card.objective.display.Decoration
import me.jfenn.bingo.common.card.objective.display.ItemStyle
import me.jfenn.bingo.common.map.CardTileImage
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.platform.IPacketBuf
import me.jfenn.bingo.platform.Identifier
import me.jfenn.bingo.platform.packet.PacketConverter
import me.jfenn.bingo.platform.readEnum
import me.jfenn.bingo.platform.text.ITextSerialized
import me.jfenn.bingo.platform.utils.UuidAsString
import java.time.Duration

data class GameMessagePacket(
    val id: UuidAsString,
    val timeElapsed: Duration,
    val team: BingoTeamKey,
    val image: CardTileImage = CardTileImage.EMPTY,
    val imageList: List<CardTileImage> = emptyList(),
    val imageStyle: ItemStyle = ItemStyle.NORMAL,
    val decoration: Decoration = Decoration.NONE,
    val decorationTexture: String? = null,
    val messageType: MessageType,
    val message: ITextSerialized,
    val isUpdate: Boolean = false,
) {
    enum class MessageType {
        LEADING_TEAM,
        LINE_SCORED,
        ITEM_SCORED,
        CARD_COMPLETED,
    }

    object V1 : PacketConverter<GameMessagePacket> {
        override val id: Identifier = Identifier.of(MOD_ID_BINGO, "game_message")

        override fun toPacketBuf(source: GameMessagePacket, dest: IPacketBuf) {
            dest.writeUUID(source.id)
            dest.writeDuration(source.timeElapsed)
            dest.writeString(source.team.id)

            CardTileImage.V1.toPacketBuf(source.image, dest)
            dest.writeList(source.imageList) {
                CardTileImage.V1.toPacketBuf(it, dest)
            }
            dest.writeEnum(source.imageStyle)
            dest.writeEnum(source.decoration)
            dest.writeNullable(source.decorationTexture, dest::writeString)

            dest.writeEnum(source.messageType)
            dest.writeText(source.message)
            dest.writeBoolean(source.isUpdate)
        }

        override fun fromPacketBuf(buf: IPacketBuf): GameMessagePacket {
            val id = buf.readUUID()
            val timeElapsed = buf.readDuration()
            val team = BingoTeamKey(buf.readString())

            val image = CardTileImage.V1.fromPacketBuf(buf)
            val imageList = buf.readList { CardTileImage.V1.fromPacketBuf(buf) }
            val imageStyle = buf.readEnum<ItemStyle>()
            val decoration = buf.readEnum<Decoration>()
            val decorationTexture = buf.readNullable { buf.readString() }

            val messageType = buf.readEnum<MessageType>()
            val message = buf.readText()

            val isUpdate = buf.readBoolean()

            return GameMessagePacket(
                id = id,
                timeElapsed = timeElapsed,
                team = team,
                image = image,
                imageList = imageList,
                imageStyle = imageStyle,
                decoration = decoration,
                decorationTexture = decorationTexture,
                messageType = messageType,
                message = message,
                isUpdate = isUpdate,
            )
        }
    }
}