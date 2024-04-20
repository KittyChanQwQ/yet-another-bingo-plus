package me.jfenn.bingo.client.impl

import me.jfenn.bingo.api.IItemStackFactory
import me.jfenn.bingo.client.api.ClientPacket
import me.jfenn.bingo.client.api.IClientNetworking
import me.jfenn.bingo.client.api.IClientPacketHandlerC2S
import me.jfenn.bingo.client.api.IClientPacketHandlerS2C
import me.jfenn.bingo.common.event.packet.PacketConverter
import me.jfenn.bingo.common.utils.EventListener
import me.jfenn.bingo.impl.PacketBufImpl
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs

class ClientNetworkingImpl(
    private val itemStackFactory: IItemStackFactory,
) : IClientNetworking {
    override fun <T> registerC2S(converter: PacketConverter<T>): IClientPacketHandlerC2S<T>
        = ClientPacketHandlerC2S(converter)

    override fun <T> registerS2C(converter: PacketConverter<T>): IClientPacketHandlerS2C<T>
        = ClientPacketHandlerS2C(converter)

    inner class ClientPacketHandlerC2S<T>(
        private val converter: PacketConverter<T>
    ) : IClientPacketHandlerC2S<T> {
        override fun isSupported(): Boolean {
            return ClientPlayNetworking.canSend(converter.id)
        }

        override fun send(packet: T): Boolean {
            if (isSupported()) {
                val buf = PacketByteBufs.create()
                converter.toPacketBuf(packet, PacketBufImpl(buf, itemStackFactory))
                ClientPlayNetworking.send(converter.id, buf)
                return true
            } else {
                return false
            }
        }
    }

    inner class ClientPacketHandlerS2C<T>(
        private val converter: PacketConverter<T>
    ) : IClientPacketHandlerS2C<T> {
        private val event = EventListener<ClientPacket<T>>()

        override fun onPacket(receiver: (ClientPacket<T>) -> Unit) = event(receiver)

        init {
            ClientPlayNetworking.registerGlobalReceiver(converter.id) { client, _, buf, _ ->
                val packet = converter.fromPacketBuf(PacketBufImpl(buf, itemStackFactory))
                val data = ClientPacket(packet)
                client.execute {
                    event(data)
                }
            }
        }
    }
}
