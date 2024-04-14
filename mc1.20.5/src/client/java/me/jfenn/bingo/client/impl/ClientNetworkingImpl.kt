package me.jfenn.bingo.client.impl

import me.jfenn.bingo.api.ITextSerializer
import me.jfenn.bingo.client.api.ClientPacket
import me.jfenn.bingo.client.api.IClientNetworking
import me.jfenn.bingo.client.api.IClientPacketHandlerC2S
import me.jfenn.bingo.client.api.IClientPacketHandlerS2C
import me.jfenn.bingo.common.event.packet.PacketConverter
import me.jfenn.bingo.common.utils.EventListener
import me.jfenn.bingo.impl.PacketBufImpl
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload

class ClientNetworkingImpl(
    private val textSerializer: ITextSerializer,
) : IClientNetworking {
    override fun <T> registerC2S(converter: PacketConverter<T>): IClientPacketHandlerC2S<T>
        = ClientPacketHandlerC2S(converter, textSerializer)

    override fun <T> registerS2C(converter: PacketConverter<T>): IClientPacketHandlerS2C<T>
        = ClientPacketHandlerS2C(converter, textSerializer)
}

class BingoPayload<T>(
    private val id: CustomPayload.Id<BingoPayload<T>>,
    internal val value: T,
) : CustomPayload {
    override fun getId() = id
}

class ClientPacketHandlerC2S<T>(
    private val converter: PacketConverter<T>,
    private val textSerializer: ITextSerializer,
) : IClientPacketHandlerC2S<T> {

    private val id = CustomPayload.id<BingoPayload<T>>(converter.id.toString())
    private val codec: PacketCodec<RegistryByteBuf, BingoPayload<T>> = PacketCodec.of(
        { type: BingoPayload<T>, buf: RegistryByteBuf -> converter.toPacketBuf(type.value, PacketBufImpl(buf, textSerializer)) },
        { b: RegistryByteBuf -> BingoPayload(id, converter.fromPacketBuf(PacketBufImpl(b, textSerializer))) }
    )

    init {
        try {
            PayloadTypeRegistry.playC2S().register(id, codec)
        } catch (e: IllegalArgumentException) {
            if (e.message?.contains("already registered") != true)
                throw RuntimeException("Error initializing bingo ClientNetworking", e)
        }
    }

    override fun isSupported(): Boolean {
        return ClientPlayNetworking.canSend(id)
    }

    override fun send(packet: T): Boolean {
        if (isSupported()) {
            ClientPlayNetworking.send(BingoPayload(id, packet))
            return true
        } else {
            return false
        }
    }
}

class ClientPacketHandlerS2C<T>(
    private val converter: PacketConverter<T>,
    private val textSerializer: ITextSerializer,
) : IClientPacketHandlerS2C<T> {
    private val event = EventListener<ClientPacket<T>>()

    private val id = CustomPayload.id<BingoPayload<T>>(converter.id.toString())
    private val codec: PacketCodec<RegistryByteBuf, BingoPayload<T>> = PacketCodec.of(
        { type: BingoPayload<T>, buf: RegistryByteBuf -> converter.toPacketBuf(type.value, PacketBufImpl(buf, textSerializer)) },
        { b: RegistryByteBuf -> BingoPayload(id, converter.fromPacketBuf(PacketBufImpl(b, textSerializer))) }
    )

    init {
        try {
            PayloadTypeRegistry.playS2C().register(id, codec)
        } catch (e: IllegalArgumentException) {
            if (e.message?.contains("already registered") != true)
                throw RuntimeException("Error initializing bingo ClientNetworking", e)
        }

        ClientPlayNetworking.registerGlobalReceiver(id) { packet, ctx ->
            val client = ctx.client()
            val data = ClientPacket(packet.value)
            client.execute {
                event(data)
            }
        }
    }

    override fun onPacket(receiver: (ClientPacket<T>) -> Unit) = event(receiver)
}
