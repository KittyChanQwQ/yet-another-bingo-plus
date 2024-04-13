package me.jfenn.bingo.client.common.packet

import me.jfenn.bingo.common.event.packet.PacketConverter
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import org.slf4j.Logger

class ClientPacketManager(
    private val log: Logger,
) {

    companion object {
        private const val VERBOSE = false
    }

    fun <T> sendPacketC2S(vararg converters: PacketConverter<T>, value: T) : Boolean {
        for (converter in converters) {
            if (!ClientPlayNetworking.canSend(converter.id)) {
                log.debug("[ClientPacketManager] Not sending unsupported packet {} to server", converter.id)
                continue
            }
            if (VERBOSE) {
                log.debug("[ClientPacketManager] Sending packet {} to server", converter.id)
            }

            ClientPlayNetworking.send(converter.id, converter.toPacketBuf(value))
            return true
        }

        return false
    }

    fun <T> registerGlobalReceiver(
        vararg converters: PacketConverter<T>,
        callback: (client: MinecraftClient, handler: ClientPlayNetworkHandler, packet: T, response: PacketSender) -> Unit
    ) {
        for (converter in converters) {
            ClientPlayNetworking.registerGlobalReceiver(converter.id) { client, handler, buf, response ->
                if (VERBOSE) {
                    log.debug("[ClientPacketManager] Received packet {} from server", converter.id)
                }

                try {
                    val packet = converter.fromPacketBuf(buf)
                    client.execute {
                        try {
                            callback(client, handler, packet, response)
                        } catch (e: Throwable) {
                            log.error("[ClientPacketManager] Error in packet callback ${converter.id}:", e)
                        }
                    }
                } catch (e: Throwable) {
                    log.error("[ClientPacketManager] Error receiving packet ${converter.id}:", e)
                }
            }
        }
    }

}