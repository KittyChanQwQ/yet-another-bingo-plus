package me.jfenn.bingo.impl.networking

import me.jfenn.bingo.impl.PlayerHandle
import me.jfenn.bingo.impl.toMinecraftIdentifier
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.item.IItemStackFactory
import me.jfenn.bingo.platform.packet.*
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import org.slf4j.Logger

class ServerNetworkingImpl(
    private val log: Logger,
    private val itemStackFactory: IItemStackFactory,
    private val eventBus: IEventBus,
) : IServerNetworking {

    override fun <T> registerC2S(converter: PacketConverter<T>): IServerPacketHandlerC2S<T>
        = ServerPacketHandlerC2S(converter)

    override fun <T> registerS2C(converter: PacketConverter<T>): IServerPacketHandlerS2C<T>
        = ServerPacketHandlerS2C(converter)

    inner class ServerPacketHandlerC2S<T>(
        private val converter: PacketConverter<T>,
    ) : IServerPacketHandlerC2S<T> {
        override val name: String = "packetC2S=${converter.id}"

        private val id = CustomPacketPayload.Type<BingoPayload<T>>(converter.id.toMinecraftIdentifier())
        private val codec: StreamCodec<RegistryFriendlyByteBuf, BingoPayload<T>> = StreamCodec.ofMember(
            { type: BingoPayload<T>, buf: RegistryFriendlyByteBuf -> converter.toPacketBuf(type.value, PacketBufImpl(buf, itemStackFactory)) },
            { b: RegistryFriendlyByteBuf -> BingoPayload(id, converter.fromPacketBuf(PacketBufImpl(b, itemStackFactory))) }
        )

        private fun receivePacket(packet: BingoPayload<T>, ctx: ServerPlayNetworking.Context) {
            val player = PlayerHandle(ctx.player())
            val data = ServerPacket(player, packet.value)
            ctx.server().execute {
                eventBus.emit(this, data)
            }
        }

        init {
            log.debug("[ServerNetworkingInfo] Registering C2S packet {}", converter.id.toString())

            try {
                PayloadTypeRegistry.serverboundPlay().register(id, codec)
            } catch (e: IllegalArgumentException) {
                if (e.message?.contains("already registered") != true)
                    throw RuntimeException("Error initializing bingo ServerNetworking", e)
            }

            ServerPlayNetworking.registerGlobalReceiver(id, ::receivePacket)
        }
    }

    inner class ServerPacketHandlerS2C<T>(
        private val converter: PacketConverter<T>,
    ) : IServerPacketHandlerS2C<T> {

        private val id = CustomPacketPayload.Type<BingoPayload<T>>(converter.id.toMinecraftIdentifier())
        private val codec: StreamCodec<RegistryFriendlyByteBuf, BingoPayload<T>> = StreamCodec.ofMember(
            { type: BingoPayload<T>, buf: RegistryFriendlyByteBuf -> converter.toPacketBuf(type.value, PacketBufImpl(buf, itemStackFactory)) },
            { b: RegistryFriendlyByteBuf -> BingoPayload(id, converter.fromPacketBuf(PacketBufImpl(b, itemStackFactory))) }
        )

        init {
            log.debug("[ServerNetworkingInfo] Registering S2C packet {}", converter.id.toString())
            try {
                PayloadTypeRegistry.clientboundPlay().register(id, codec)
            } catch (e: IllegalArgumentException) {
                if (e.message?.contains("already registered") != true)
                    throw RuntimeException("Error initializing bingo ServerNetworking", e)
            }
        }

        override fun isSupported(player: ServerPlayer): Boolean {
            return ServerPlayNetworking.canSend(player, id)
        }

        override fun send(player: ServerPlayer, packet: T): Boolean {
            if (ServerPlayNetworking.canSend(player, id)) {
                ServerPlayNetworking.send(player, BingoPayload(id, packet))
                return true
            } else {
                return false
            }
        }
    }
}
