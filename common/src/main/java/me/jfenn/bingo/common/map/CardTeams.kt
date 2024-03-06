package me.jfenn.bingo.common.map

import me.jfenn.bingo.common.card.BingoTeamKey
import me.jfenn.bingo.common.utils.text.TextColor
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.network.PacketByteBuf

data class CardTeams(
    val teamInfo: Map<BingoTeamKey, TextColor>,
    val display: List<BingoTeamKey?>,
) {

    fun toPacketBuf(): PacketByteBuf {
        val buf = PacketByteBufs.create()

        buf.writeInt(teamInfo.size)
        for ((key, value) in teamInfo) {
            buf.writeString(key.id)
            buf.writeString(value.name)
        }

        buf.writeInt(display.size)
        for (key in display) {
            if (key != null) {
                buf.writeBoolean(true)
                buf.writeString(key.id)
            } else {
                buf.writeBoolean(false)
            }
        }

        return buf
    }

    companion object {
        fun fromPacketBuf(buf: PacketByteBuf): CardTeams {
            val teamInfo = List(buf.readInt()) {
                BingoTeamKey(buf.readString()) to TextColor.valueOf(buf.readString())
            }.toMap()

            val display = List(buf.readInt()) {
                if (buf.readBoolean())
                    BingoTeamKey(buf.readString())
                else null
            }

            return CardTeams(
                teamInfo = teamInfo,
                display = display,
            )
        }
    }

}