package me.jfenn.bingo.common.event.packet

import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier

interface PacketConverter<T> {

    val id: Identifier

    fun toPacketBuf(source: T): PacketByteBuf

    fun fromPacketBuf(buf: PacketByteBuf): T

}