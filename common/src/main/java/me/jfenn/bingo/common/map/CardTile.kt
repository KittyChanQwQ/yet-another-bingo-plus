package me.jfenn.bingo.common.map

import me.jfenn.bingo.api.IPacketBuf
import me.jfenn.bingo.common.config.TierLabel
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Formatting

data class CardTile(
    val item: ItemStack?,
    val itemTier: TierLabel?,
    val isFreeSpace: Boolean,
    val isAdvancement: Boolean,
    val isMultiItem: Boolean,
    val isHidden: Boolean,
    val isLocked: Boolean,
    val isFlashing: Boolean,
    val isAchieved: Boolean,
    val teamColors: List<Formatting>,
) {
    fun toPacketBuf(buf: IPacketBuf) {
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

        buf.writeInt(teamColors.size)
        for (team in teamColors) {
            buf.writeString(team.name)
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
                teamColors = List(buf.readInt()) { Formatting.valueOf(buf.readString()) },
            )
        }
    }
}
