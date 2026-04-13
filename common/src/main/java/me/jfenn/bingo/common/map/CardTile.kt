package me.jfenn.bingo.common.map

import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.platform.IPacketBuf
import me.jfenn.bingo.platform.Identifier
import me.jfenn.bingo.platform.packet.PacketConverter
import me.jfenn.bingo.platform.text.IText
import java.time.Instant

data class CardTile(
    val id: String? = null,
    val image: CardTileImage,
    val imageList: List<CardTileImage> = emptyList(),
    val action: CardTileAction = image.item?.let { CardTileAction.Item(it) }
        ?: CardTileAction.None,
    val itemTier: TierLabel? = null,
    val name: IText? = image.item?.displayName,
    val lore: List<IText> = image.item?.lore ?: emptyList(),
    val decoration: Decoration?,
    val isHidden: Boolean = false,
    val isLocked: Boolean = false,
    /**
     * True if the tile should be rendered as flashing in the current tick
     */
    val isFlashingOnMap: Boolean = false,
    /**
     * True if the tile has been recently scored
     */
    val isFlashing: Boolean? = null,
    val isAchieved: Boolean = false,
    val progress: Float = 0f,
    val teamKeys: List<BingoTeamKey> = emptyList(),
) {

    @Transient
    var updatedAt: Instant? = null

    @Transient
    internal val mapRenderKey = image.item?.let { item ->
        MapRenderService.TileImageKey(
            name = item.identifier.toString(),
            image = image.mapTexture ?: image.texture,
            count = item.count,
            decoration = decoration,
        )
    }

    @Transient
    var clientTooltip: List<IText>? = null

    enum class Decoration {
        FREE_SPACE,
        ADVANCEMENT,
        ONE_OF,
        MANY_OF,
        @Deprecated("Use ONE_OF or MANY_OF instead!")
        MULTI_ITEM,
        FORBIDDEN,
        NONE
    }

    object V1 : PacketConverter<CardTile> {
        override val id: Identifier = Identifier.of(MOD_ID_BINGO, "card_tile")

        override fun fromPacketBuf(buf: IPacketBuf): CardTile {
            val id = buf.readString().takeIf { it.isNotEmpty() }
            val image = CardTileImage.V1.fromPacketBuf(buf)
            val imageList = buf.readList { CardTileImage.V1.fromPacketBuf(buf) }
            val action = CardTileAction.V1.fromPacketBuf(buf)
            val itemTier = buf.readNullable {
                TierLabel.valueOf(buf.readString())
            }
            val name = buf.readNullable(buf::readText)
            val lore = List(buf.readInt()) { buf.readText() }
            val decoration = try {
                Decoration.valueOf(buf.readString())
            } catch (e: IllegalArgumentException) {
                null
            }
            val isHidden = buf.readBoolean()
            val isLocked = buf.readBoolean()
            val isFlashing = buf.readBoolean()
            val isAchieved = buf.readBoolean()
            val progress = buf.readFloat()
            val teamKeys = List(buf.readInt()) {
                BingoTeamKey(buf.readString())
            }
            return CardTile(
                id = id,
                image = image,
                imageList = imageList,
                action = action,
                itemTier = itemTier,
                name = name,
                lore = lore,
                decoration = decoration,
                isHidden = isHidden,
                isLocked = isLocked,
                isFlashing = isFlashing,
                isAchieved = isAchieved,
                progress = progress,
                teamKeys = teamKeys,
            )
        }

        override fun toPacketBuf(source: CardTile, dest: IPacketBuf) {
            dest.writeString(source.id.orEmpty())
            CardTileImage.V1.toPacketBuf(source.image, dest)
            dest.writeList(source.imageList) {
                CardTileImage.V1.toPacketBuf(it, dest)
            }
            CardTileAction.V1.toPacketBuf(source.action, dest)
            dest.writeNullable(source.itemTier?.name, dest::writeString)
            dest.writeNullable(source.name, dest::writeText)

            dest.writeInt(source.lore.size)
            for (text in source.lore) {
                dest.writeText(text)
            }

            dest.writeString(source.decoration?.name ?: "")

            dest.writeBoolean(source.isHidden)
            dest.writeBoolean(source.isLocked)
            dest.writeBoolean(source.isFlashing ?: false)
            dest.writeBoolean(source.isAchieved)

            dest.writeFloat(source.progress)

            dest.writeInt(source.teamKeys.size)
            for (team in source.teamKeys) {
                dest.writeString(team.id)
            }
        }
    }

    companion object {
        val EMPTY = CardTile(
            image = CardTileImage(
                item = null,
                texture = null,
            ),
            itemTier = null,
            decoration = null,
            isHidden = false,
            isLocked = false,
            isFlashing = false,
            isAchieved = false,
            teamKeys = emptyList(),
        )

        val MULTI_ITEM_ENUMS = arrayOf(Decoration.ONE_OF, Decoration.MANY_OF, @Suppress("Deprecation") Decoration.MULTI_ITEM)
    }
}
