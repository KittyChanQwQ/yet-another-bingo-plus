package me.jfenn.bingo.common.event.packet

import me.jfenn.bingo.api.IPacketBuf
import net.minecraft.util.Identifier

interface PacketConverter<T> {

    val id: Identifier

    fun toPacketBuf(source: T, dest: IPacketBuf)

    fun fromPacketBuf(buf: IPacketBuf): T

}