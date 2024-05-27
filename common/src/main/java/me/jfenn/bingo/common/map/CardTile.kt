package me.jfenn.bingo.common.map

import me.jfenn.bingo.api.IPacketBuf
import me.jfenn.bingo.api.item.IItemStack
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.config.TierLabel

data class CardTile(
    val item: IItemStack?,
    val itemTier: TierLabel?,
    val isFreeSpace: Boolean,
    val isAdvancement: Boolean,
    val isMultiItem: Boolean,
    val isHidden: Boolean,
    val isLocked: Boolean,
    val isFlashing: Boolean,
    val isAchieved: Boolean,
    val teamKeys: List<BingoTeamKey>,
) {
    fun toPacketBufV1(buf: IPacketBuf) {
        if (item != null) {
            buf.writeBoolean(true)
            buf.writeItemStack(item)
        } else {
            buf.writeBoolean(false)
        }

        if (itemTier != null) {
            buf.writeBoolean(true)
            buf.writeString(itemTier.name)
        } else {
            buf.writeBoolean(false)
        }

        buf.writeBoolean(isFreeSpace)
        buf.writeBoolean(isAdvancement)
        buf.writeBoolean(isMultiItem)
        buf.writeBoolean(isHidden)
        buf.writeBoolean(isLocked)
        buf.writeBoolean(isFlashing)
        buf.writeBoolean(isAchieved)

        buf.writeInt(teamKeys.size)
        for (team in teamKeys) {
            val formatting = when (
                val color = team.label.uppercase()
            ) {
                "PINK" -> "LIGHT_PURPLE"
                "ORANGE" -> "GOLD"
                "GRAY" -> "DARK_GRAY"
                else -> color
            }
            buf.writeString(formatting)
        }
    }

    fun toPacketBufV2(buf: IPacketBuf) {
        if (item != null) {
            buf.writeBoolean(true)
            buf.writeItemStack(item)
        } else {
            buf.writeBoolean(false)
        }

        if (itemTier != null) {
            buf.writeBoolean(true)
            buf.writeString(itemTier.name)
        } else {
            buf.writeBoolean(false)
        }

        buf.writeBoolean(isFreeSpace)
        buf.writeBoolean(isAdvancement)
        buf.writeBoolean(isMultiItem)
        buf.writeBoolean(isHidden)
        buf.writeBoolean(isLocked)
        buf.writeBoolean(isFlashing)
        buf.writeBoolean(isAchieved)

        buf.writeInt(teamKeys.size)
        for (team in teamKeys) {
            buf.writeString(team.id)
        }
    }

    companion object {
        fun fromPacketBuf(buf: IPacketBuf): CardTile {
            return CardTile(
                item = if (buf.readBoolean()) {
                    buf.readItemStack()
                } else null,
                itemTier = if (buf.readBoolean()) {
                    TierLabel.valueOf(buf.readString())
                } else null,
                isFreeSpace = buf.readBoolean(),
                isAdvancement = buf.readBoolean(),
                isMultiItem = buf.readBoolean(),
                isHidden = buf.readBoolean(),
                isLocked = buf.readBoolean(),
                isFlashing = buf.readBoolean(),
                isAchieved = buf.readBoolean(),
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
    }
}
