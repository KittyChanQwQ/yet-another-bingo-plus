package me.jfenn.bingo.common.menu.tooltips

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jfenn.bingo.client.platform.text.IText
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.platform.IPacketBuf
import me.jfenn.bingo.platform.packet.PacketConverter
import net.minecraft.util.Identifier

class TooltipPacket(
    val text: List<IText>,
    val createdAt: Instant = Clock.System.now(),
) {

    @Transient
    var clientWrappedText: List<IText>? = null

    object V1 : PacketConverter<TooltipPacket> {
        override val id: Identifier = Identifier.of(MOD_ID_BINGO, "tooltip")!!

        override fun fromPacketBuf(buf: IPacketBuf): TooltipPacket {
            val tooltip = List(buf.readInt()) { buf.readText() }
            return TooltipPacket(tooltip)
        }

        override fun toPacketBuf(source: TooltipPacket, dest: IPacketBuf) {
            dest.writeInt(source.text.size)
            source.text.forEach { dest.writeText(it) }
        }
    }
}