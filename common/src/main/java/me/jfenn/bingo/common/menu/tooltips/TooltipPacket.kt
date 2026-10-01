package me.jfenn.bingo.common.menu.tooltips

import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.platform.IPacketBuf
import me.jfenn.bingo.platform.Identifier
import me.jfenn.bingo.platform.packet.PacketConverter
import me.jfenn.bingo.platform.text.IText
import java.util.*

class TooltipPacket(
    val text: List<IText>,
    val entityId: UUID,
) {

    @Transient
    var clientWrappedText: List<IText>? = null

    object V1 : PacketConverter<TooltipPacket> {
        override val id: Identifier = Identifier.of(MOD_ID_BINGO, "tooltip_v2")

        override fun fromPacketBuf(buf: IPacketBuf): TooltipPacket {
            return TooltipPacket(
                text = buf.readList(buf::readText),
                entityId = buf.readUUID(),
            )
        }

        override fun toPacketBuf(source: TooltipPacket, dest: IPacketBuf) {
            dest.writeList(source.text, dest::writeText)
            dest.writeUUID(source.entityId)
        }
    }
}