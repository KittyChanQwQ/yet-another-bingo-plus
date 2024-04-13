package me.jfenn.bingo.common.event.packet

import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import org.slf4j.Logger

class ServerPacketManager(
    private val log: Logger,
) {

    fun <T> sendPacketS2C(player: ServerPlayerEntity, converter: PacketConverter<T>, value: T) : Boolean {
        return if (ServerPlayNetworking.canSend(player, converter.id)) {
            log.debug("[ServerPacketManager] Sending packet {} to client", converter.id)
            ServerPlayNetworking.send(player, converter.id, converter.toPacketBuf(value))
            true
        } else {
            log.debug("[ServerPacketManager] Not sending unsupported packet {} to client", converter.id)
            false
        }
    }

    fun <T> registerGlobalReceiver(
        converter: PacketConverter<T>,
        callback: (server: MinecraftServer, player: ServerPlayerEntity, handler: ServerPlayNetworkHandler, packet: T, response: PacketSender) -> Unit
    ) {
        ServerPlayNetworking.registerGlobalReceiver(converter.id) { server, player, handler, buf, response ->
            log.debug("[ServerPacketManager] Received packet {} from client", converter.id)
            try {
                val packet = converter.fromPacketBuf(buf)
                server.execute {
                    try {
                        callback(server, player, handler, packet, response)
                    } catch (e: Throwable) {
                        log.error("[ServerPacketManager] Error in packet callback ${converter.id}:", e)
                    }
                }
            } catch (e: Throwable) {
                log.error("[ServerPacketManager] Error receiving packet ${converter.id}:", e)
            }
        }
    }

}