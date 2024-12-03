package me.jfenn.bingo.common.map

import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.platform.IPacketBuf
import me.jfenn.bingo.platform.item.IItemStack
import me.jfenn.bingo.platform.packet.PacketConverter
import net.minecraft.util.Identifier

data class CardTileImage(
    val item: IItemStack? = null,
    val texture: String? = null,
) {

    object V1 : PacketConverter<CardTileImage> {
        override val id: Identifier = Identifier.of(MOD_ID_BINGO, "card_tile_image")!!

        override fun fromPacketBuf(buf: IPacketBuf): CardTileImage {
            return CardTileImage(
                item = buf.readNullable(buf::readItemStack),
                texture = buf.readNullable(buf::readString)
            )
        }

        override fun toPacketBuf(source: CardTileImage, dest: IPacketBuf) {
            dest.writeNullable(source.item, dest::writeItemStack)
            dest.writeNullable(source.texture, dest::writeString)
        }
    }
}