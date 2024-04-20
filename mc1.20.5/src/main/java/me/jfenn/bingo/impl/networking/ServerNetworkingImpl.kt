package me.jfenn.bingo.impl.networking

import me.jfenn.bingo.api.*
import me.jfenn.bingo.api.item.IItemStackFactory
import me.jfenn.bingo.common.event.packet.PacketConverter
import me.jfenn.bingo.common.utils.EventListener
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.server.network.ServerPlayerEntity

class ServerNetworkingImpl(
    private val itemStackFactory: IItemStackFactory,
) : IServerNetworking {

    override fun <T> registerC2S(converter: PacketConverter<T>): IServerPacketHandlerC2S<T>
        = ServerPacketHandlerC2S(converter)

    override fun <T> registerS2C(converter: PacketConverter<T>): IServerPacketHandlerS2C<T>
        = ServerPacketHandlerS2C(converter)

    inner class ServerPacketHandlerC2S<T>(
        private val converter: PacketConverter<T>,
    ) : IServerPacketHandlerC2S<T> {
        private val event = EventListener<ServerPacket<T>>()

        private val id = CustomPayload.id<BingoPayload<T>>(converter.id.toString())
        private val codec: PacketCodec<RegistryByteBuf, BingoPayload<T>> = PacketCodec.of(
            { type: BingoPayload<T>, buf: RegistryByteBuf -> converter.toPacketBuf(type.value, PacketBufImpl(buf, itemStackFactory)) },
            { b: RegistryByteBuf -> BingoPayload(id, converter.fromPacketBuf(PacketBufImpl(b, itemStackFactory))) }
        )

        init {
            try {
                PayloadTypeRegistry.playC2S().register(id, codec)
            } catch (e: IllegalArgumentException) {
                if (e.message?.contains("already registered") != true)
                    throw RuntimeException("Error initializing bingo ServerNetworking", e)
            }

            ServerPlayNetworking.registerGlobalReceiver(id) { packet, ctx ->
                val player = ctx.player()
                val data = ServerPacket(player, packet.value)
                player.server.execute {
                    event(data)
                }
            }
        }

        override fun onPacket(receiver: (ServerPacket<T>) -> Unit) = event(receiver)
    }

    inner class ServerPacketHandlerS2C<T>(
        private val converter: PacketConverter<T>,
    ) : IServerPacketHandlerS2C<T> {

        private val id = CustomPayload.id<BingoPayload<T>>(converter.id.toString())
        private val codec: PacketCodec<RegistryByteBuf, BingoPayload<T>> = PacketCodec.of(
            { type: BingoPayload<T>, buf: RegistryByteBuf -> converter.toPacketBuf(type.value, PacketBufImpl(buf, itemStackFactory)) },
            { b: RegistryByteBuf -> BingoPayload(id, converter.fromPacketBuf(PacketBufImpl(b, itemStackFactory))) }
        )

        init {
            try {
                PayloadTypeRegistry.playS2C().register(id, codec)
            } catch (e: IllegalArgumentException) {
                if (e.message?.contains("already registered") != true)
                    throw RuntimeException("Error initializing bingo ServerNetworking", e)
            }
        }

        override fun isSupported(player: ServerPlayerEntity): Boolean {
            return ServerPlayNetworking.canSend(player, id)
        }

        override fun send(player: ServerPlayerEntity, packet: T): Boolean {
            if (isSupported(player)) {
                ServerPlayNetworking.send(player, BingoPayload(id, packet))
                return true
            } else {
                return false
            }
        }
    }
}
