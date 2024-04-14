package me.jfenn.bingo.api

import me.jfenn.bingo.common.event.packet.PacketConverter
import net.minecraft.server.network.ServerPlayerEntity

interface IServerNetworking {
    fun <T> registerC2S(converter: PacketConverter<T>): IServerPacketHandlerC2S<T>
    fun <T> registerS2C(converter: PacketConverter<T>): IServerPacketHandlerS2C<T>
}

interface IServerPacketHandlerC2S<T> {
    fun onPacket(receiver: (ServerPacket<T>) -> Unit)
}

interface IServerPacketHandlerS2C<T> {
    fun isSupported(player: ServerPlayerEntity): Boolean
    fun send(player: ServerPlayerEntity, packet: T): Boolean
}

data class ServerPacket<T>(
    val player: ServerPlayerEntity,
    val packet: T
)
