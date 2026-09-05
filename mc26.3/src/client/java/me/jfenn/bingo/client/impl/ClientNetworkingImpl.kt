package me.jfenn.bingo.client.impl

import me.jfenn.bingo.client.platform.ClientPacket
import me.jfenn.bingo.client.platform.IClientNetworking
import me.jfenn.bingo.client.platform.IClientPacketHandlerC2S
import me.jfenn.bingo.client.platform.IClientPacketHandlerS2C
import me.jfenn.bingo.impl.networking.BingoPayload
import me.jfenn.bingo.impl.networking.PacketBufImpl
import me.jfenn.bingo.impl.toMinecraftIdentifier
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.item.IItemStackFactory
import me.jfenn.bingo.platform.packet.PacketConverter
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import org.slf4j.Logger

class ClientNetworkingImpl(
    private val log: Logger,
    private val itemStackFactory: IItemStackFactory,
    private val eventBus: IEventBus,
) : IClientNetworking {

    override fun <T> registerC2S(converter: PacketConverter<T>): IClientPacketHandlerC2S<T>
        = ClientPacketHandlerC2S(converter)

    override fun <T> registerS2C(converter: PacketConverter<T>): IClientPacketHandlerS2C<T>
        = ClientPacketHandlerS2C(converter)

    inner class ClientPacketHandlerC2S<T>(
        private val converter: PacketConverter<T>,
    ) : IClientPacketHandlerC2S<T> {

        private val id = CustomPacketPayload.Type<BingoPayload<T>>(converter.id.toMinecraftIdentifier())
        private val codec: StreamCodec<RegistryFriendlyByteBuf, BingoPayload<T>> = StreamCodec.ofMember(
            { type: BingoPayload<T>, buf: RegistryFriendlyByteBuf -> converter.toPacketBuf(type.value, PacketBufImpl(buf, itemStackFactory)) },
            { b: RegistryFriendlyByteBuf -> BingoPayload(id, converter.fromPacketBuf(PacketBufImpl(b, itemStackFactory))) }
        )

        init {
            log.debug("[ClientNetworkingImpl] Registering C2S packet {}", converter.id.toString())
            try {
                PayloadTypeRegistry.serverboundPlay().register(id, codec)
            } catch (e: IllegalArgumentException) {
                if (e.message?.contains("already registered") != true)
                    throw RuntimeException("Error initializing bingo ClientNetworking", e)
            }
        }

        override fun isSupported(): Boolean {
            return ClientPlayNetworking.canSend(id)
        }

        override fun send(packet: T): Boolean {
            if (ClientPlayNetworking.canSend(id)) {
                ClientPlayNetworking.send(BingoPayload(id, packet))
                return true
            } else {
                return false
            }
        }
    }

    inner class ClientPacketHandlerS2C<T>(
        private val converter: PacketConverter<T>,
    ) : IClientPacketHandlerS2C<T> {
        override val name: String = "packetS2C=${converter.id}"

        private val id = CustomPacketPayload.Type<BingoPayload<T>>(converter.id.toMinecraftIdentifier())
        private val codec: StreamCodec<RegistryFriendlyByteBuf, BingoPayload<T>> = StreamCodec.ofMember(
            { type: BingoPayload<T>, buf: RegistryFriendlyByteBuf -> converter.toPacketBuf(type.value, PacketBufImpl(buf, itemStackFactory)) },
            { b: RegistryFriendlyByteBuf -> BingoPayload(id, converter.fromPacketBuf(PacketBufImpl(b, itemStackFactory))) }
        )

        private fun receivePacket(packet: BingoPayload<T>, ctx: ClientPlayNetworking.Context) {
            val client = ctx.client()
            val data = ClientPacket(packet.value)
            client.execute {
                eventBus.emit(this, data)
            }
        }

        init {
            log.debug("[ClientNetworkingImpl] Registering S2C packet {}", converter.id.toString())

            try {
                PayloadTypeRegistry.clientboundPlay().register(id, codec)
            } catch (e: IllegalArgumentException) {
                if (e.message?.contains("already registered") != true)
                    throw RuntimeException("Error initializing bingo ClientNetworking", e)
            }

            ClientPlayNetworking.registerGlobalReceiver(id, ::receivePacket)
        }
    }
}
