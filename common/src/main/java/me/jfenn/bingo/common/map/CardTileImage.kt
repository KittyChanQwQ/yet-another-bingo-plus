package me.jfenn.bingo.common.map

import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.card.objective.display.Decoration
import me.jfenn.bingo.common.card.objective.display.ItemStyle
import me.jfenn.bingo.common.card.objective.display.ObjectiveDisplay
import me.jfenn.bingo.platform.IPacketBuf
import me.jfenn.bingo.platform.Identifier
import me.jfenn.bingo.platform.item.IItemStackSerialized
import me.jfenn.bingo.platform.packet.PacketConverter
import me.jfenn.bingo.platform.readEnum

@Serializable
data class CardTileImage(
    val item: IItemStackSerialized? = null,
    val texture: String? = null,
    val mapTexture: String? = null,
    val itemStyle: ItemStyle = ItemStyle.NORMAL,
    val decoration: Decoration = Decoration.NONE,
    val decorationTexture: String? = null,
    val isAchieved: Boolean = false,
) {

    companion object {
        val EMPTY = CardTileImage()
    }

    override fun hashCode(): Int {
        return arrayOf(item?.identifier, texture, mapTexture).contentHashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is CardTileImage &&
                item?.identifier == other.item?.identifier &&
                texture == other.texture &&
                mapTexture == other.mapTexture
    }

    object V1 : PacketConverter<CardTileImage> {
        override val id: Identifier = Identifier.of(MOD_ID_BINGO, "card_tile_image")

        override fun fromPacketBuf(buf: IPacketBuf): CardTileImage {
            return CardTileImage(
                item = buf.readNullable(buf::readItemStack),
                texture = buf.readNullable(buf::readString),
                itemStyle = buf.readEnum(),
                decoration = buf.readEnum(),
                decorationTexture = buf.readNullable(buf::readString),
                isAchieved = buf.readBoolean(),
            )
        }

        override fun toPacketBuf(source: CardTileImage, dest: IPacketBuf) {
            dest.writeNullable(source.item, dest::writeItemStack)
            dest.writeNullable(source.texture, dest::writeString)
            dest.writeEnum(source.itemStyle)
            dest.writeEnum(source.decoration)
            dest.writeNullable(source.decorationTexture, dest::writeString)
            dest.writeBoolean(source.isAchieved)
        }
    }
}

internal fun ObjectiveDisplay.Resolved.toCardTileImage() = CardTileImage(
    item = item,
    texture = image,
    mapTexture = mapImage,
    itemStyle = itemStyle,
    decoration = decoration,
    decorationTexture = decorationTexture,
)
