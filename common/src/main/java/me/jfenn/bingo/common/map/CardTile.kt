package me.jfenn.bingo.common.map

import me.jfenn.bingo.api.IPacketBuf
import me.jfenn.bingo.api.item.IItemStack
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.event.packet.PacketConverter
import me.jfenn.bingo.common.team.BingoTeamKey
import net.minecraft.text.Text
import net.minecraft.util.Identifier

data class CardTile(
    val item: IItemStack? = null,
    val itemTier: TierLabel?,
    val image: String? = null,
    val name: Text? = item?.displayName,
    val lore: List<Text> = item?.lore ?: emptyList(),
    val decoration: Decoration?,
    val isHidden: Boolean,
    val isLocked: Boolean,
    val isFlashing: Boolean,
    val isAchieved: Boolean,
    val progress: Float = 0f,
    val teamKeys: List<BingoTeamKey>,
) {

    @Transient
    val mapRenderKey = item?.let {
        MapRenderService.TileImageKey(
            name = item.identifier.path,
            count = item.count,
            decoration = decoration,
        )
    }

    enum class Decoration {
        FREE_SPACE,
        ADVANCEMENT,
        MULTI_ITEM,
        FORBIDDEN,
        NONE
    }

    object V1 : PacketConverter<CardTile> {
        override val id: Identifier = Identifier.of(MOD_ID_BINGO, "card_tile")!!

        override fun fromPacketBuf(buf: IPacketBuf): CardTile {
            val item = if (buf.readBoolean()) {
                buf.readItemStack()
            } else null
            val  itemTier = if (buf.readBoolean()) {
                TierLabel.valueOf(buf.readString())
            } else null
            val isFreeSpace = buf.readBoolean()
            val isAdvancement = buf.readBoolean()
            val isMultiItem = buf.readBoolean()
            val isHidden = buf.readBoolean()
            val isLocked = buf.readBoolean()
            val isFlashing = buf.readBoolean()
            val isAchieved = buf.readBoolean()
            val teamKeys = List(buf.readInt()) {
                val color = buf.readString().lowercase()
                when (color) {
                    "light_purple" -> "pink"
                    "gold" -> "orange"
                    "dark_gray" -> "gray"
                    else -> color.removePrefix("bingo_")
                }.let { BingoTeamKey("bingo_$it") }
            }

            return CardTile(
                item = item,
                itemTier = itemTier,
                decoration = when {
                    isFreeSpace -> Decoration.FREE_SPACE
                    isAdvancement -> Decoration.ADVANCEMENT
                    isMultiItem -> Decoration.MULTI_ITEM
                    else -> null
                },
                isHidden = isHidden,
                isLocked = isLocked,
                isFlashing = isFlashing,
                isAchieved = isAchieved,
                teamKeys = teamKeys,
            )
        }

        override fun toPacketBuf(source: CardTile, dest: IPacketBuf) {
            if (source.item != null) {
                dest.writeBoolean(true)
                dest.writeItemStack(source.item)
            } else {
                dest.writeBoolean(false)
            }

            if (source.itemTier != null) {
                dest.writeBoolean(true)
                dest.writeString(source.itemTier.name)
            } else {
                dest.writeBoolean(false)
            }

            dest.writeBoolean(source.decoration == Decoration.FREE_SPACE)
            dest.writeBoolean(source.decoration == Decoration.ADVANCEMENT)
            dest.writeBoolean(source.decoration == Decoration.MULTI_ITEM)
            dest.writeBoolean(source.isHidden)
            dest.writeBoolean(source.isLocked || source.decoration == Decoration.FORBIDDEN)
            dest.writeBoolean(source.isFlashing)
            dest.writeBoolean(source.isAchieved)

            dest.writeInt(source.teamKeys.size)
            for (team in source.teamKeys) {
                val formatting = when (
                    val color = team.label.uppercase()
                ) {
                    "PINK" -> "LIGHT_PURPLE"
                    "ORANGE" -> "GOLD"
                    "GRAY" -> "DARK_GRAY"
                    else -> color
                }
                dest.writeString(formatting)
            }
        }
    }

    object V2 : PacketConverter<CardTile> {
        override val id: Identifier = Identifier.of(MOD_ID_BINGO, "card_tile_v2")!!

        override fun fromPacketBuf(buf: IPacketBuf): CardTile {
            return V1.fromPacketBuf(buf)
        }

        override fun toPacketBuf(source: CardTile, dest: IPacketBuf) {
            if (source.item != null) {
                dest.writeBoolean(true)
                dest.writeItemStack(source.item)
            } else {
                dest.writeBoolean(false)
            }

            if (source.itemTier != null) {
                dest.writeBoolean(true)
                dest.writeString(source.itemTier.name)
            } else {
                dest.writeBoolean(false)
            }

            dest.writeBoolean(source.decoration == Decoration.FREE_SPACE)
            dest.writeBoolean(source.decoration == Decoration.ADVANCEMENT)
            dest.writeBoolean(source.decoration == Decoration.MULTI_ITEM)
            dest.writeBoolean(source.isHidden)
            dest.writeBoolean(source.isLocked || source.decoration == Decoration.FORBIDDEN)
            dest.writeBoolean(source.isFlashing)
            dest.writeBoolean(source.isAchieved)

            dest.writeInt(source.teamKeys.size)
            for (team in source.teamKeys) {
                dest.writeString(team.id)
            }
        }
    }

    object V3 : PacketConverter<CardTile> {
        override val id: Identifier = Identifier.of(MOD_ID_BINGO, "card_tile_v3")!!

        override fun fromPacketBuf(buf: IPacketBuf): CardTile {
            return CardTile(
                item = if (buf.readBoolean()) {
                    buf.readItemStack()
                } else null,
                itemTier = if (buf.readBoolean()) {
                    TierLabel.valueOf(buf.readString())
                } else null,
                name = if (buf.readBoolean()) {
                    buf.readText()
                } else null,
                lore = List(buf.readInt()) { buf.readText() },
                decoration = try {
                    Decoration.valueOf(buf.readString())
                } catch (e: IllegalArgumentException) {
                    null
                },
                isHidden = buf.readBoolean(),
                isLocked = buf.readBoolean(),
                isFlashing = buf.readBoolean(),
                isAchieved = buf.readBoolean(),
                progress = buf.readFloat(),
                teamKeys = List(buf.readInt()) {
                    val color = buf.readString().lowercase()
                    when (color) {
                        "light_purple" -> "pink"
                        "gold" -> "orange"
                        "dark_gray" -> "gray"
                        else -> color.removePrefix("bingo_")
                    }.let { BingoTeamKey("bingo_$it") }
                },
            )
        }

        override fun toPacketBuf(source: CardTile, dest: IPacketBuf) {
            if (source.item != null) {
                dest.writeBoolean(true)
                dest.writeItemStack(source.item)
            } else {
                dest.writeBoolean(false)
            }

            if (source.itemTier != null) {
                dest.writeBoolean(true)
                dest.writeString(source.itemTier.name)
            } else {
                dest.writeBoolean(false)
            }

            if (source.name != null) {
                dest.writeBoolean(true)
                dest.writeText(source.name)
            } else {
                dest.writeBoolean(false)
            }

            dest.writeInt(source.lore.size)
            for (text in source.lore) {
                dest.writeText(text)
            }

            dest.writeString(source.decoration?.name ?: "")

            dest.writeBoolean(source.isHidden)
            dest.writeBoolean(source.isLocked)
            dest.writeBoolean(source.isFlashing)
            dest.writeBoolean(source.isAchieved)

            dest.writeFloat(source.progress)

            dest.writeInt(source.teamKeys.size)
            for (team in source.teamKeys) {
                dest.writeString(team.id)
            }
        }
    }

    object V4 : PacketConverter<CardTile> {
        override val id: Identifier = Identifier.of(MOD_ID_BINGO, "card_tile_v4")!!

        override fun fromPacketBuf(buf: IPacketBuf): CardTile {
            return CardTile(
                item = buf.readNullable(buf::readItemStack),
                itemTier = buf.readNullable {
                    TierLabel.valueOf(buf.readString())
                },
                image = buf.readNullable(buf::readString),
                name = buf.readNullable(buf::readText),
                lore = List(buf.readInt()) { buf.readText() },
                decoration = try {
                    Decoration.valueOf(buf.readString())
                } catch (e: IllegalArgumentException) {
                    null
                },
                isHidden = buf.readBoolean(),
                isLocked = buf.readBoolean(),
                isFlashing = buf.readBoolean(),
                isAchieved = buf.readBoolean(),
                progress = buf.readFloat(),
                teamKeys = List(buf.readInt()) {
                    val color = buf.readString().lowercase()
                    when (color) {
                        "light_purple" -> "pink"
                        "gold" -> "orange"
                        "dark_gray" -> "gray"
                        else -> color.removePrefix("bingo_")
                    }.let { BingoTeamKey("bingo_$it") }
                },
            )
        }

        override fun toPacketBuf(source: CardTile, dest: IPacketBuf) {
            dest.writeNullable(source.item, dest::writeItemStack)
            dest.writeNullable(source.itemTier?.name, dest::writeString)
            dest.writeNullable(source.image, dest::writeString)
            dest.writeNullable(source.name, dest::writeText)

            dest.writeInt(source.lore.size)
            for (text in source.lore) {
                dest.writeText(text)
            }

            dest.writeString(source.decoration?.name ?: "")

            dest.writeBoolean(source.isHidden)
            dest.writeBoolean(source.isLocked)
            dest.writeBoolean(source.isFlashing)
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
            item = null,
            itemTier = null,
            decoration = null,
            isHidden = false,
            isLocked = false,
            isFlashing = false,
            isAchieved = false,
            teamKeys = emptyList(),
        )
    }
}
