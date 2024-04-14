package me.jfenn.bingo.client.api

import me.jfenn.bingo.common.event.packet.PacketConverter

interface IClientNetworking {
    fun <T> registerC2S(converter: PacketConverter<T>): IClientPacketHandlerC2S<T>
    fun <T> registerS2C(converter: PacketConverter<T>): IClientPacketHandlerS2C<T>
}

interface IClientPacketHandlerC2S<T> {
    fun isSupported(): Boolean
    fun send(packet: T): Boolean
}

interface IClientPacketHandlerS2C<T> {
    fun onPacket(receiver: (ClientPacket<T>) -> Unit)
}

data class ClientPacket<T>(
    val packet: T
)
