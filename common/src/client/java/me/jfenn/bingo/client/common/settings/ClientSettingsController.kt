package me.jfenn.bingo.client.common.settings

import me.jfenn.bingo.common.PACKET_SETTINGS_UPDATE_C2S
import me.jfenn.bingo.common.PACKET_SETTINGS_UPDATE_S2C
import me.jfenn.bingo.common.config.PlayerSettings
import net.fabricmc.fabric.api.client.networking.v1.C2SPlayChannelEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import org.slf4j.Logger

class ClientSettingsController(
    private val log: Logger,
    private val clientSettings: ClientSettingsService,
) {

    init {
        ClientPlayNetworking.registerGlobalReceiver(PACKET_SETTINGS_UPDATE_S2C) { client, handler, buf, response ->
            log.debug("[ClientSettingsController] Received packet {} from server", PACKET_SETTINGS_UPDATE_S2C)
            val settings = PlayerSettings.fromPacketBuf(buf) ?: return@registerGlobalReceiver
            client.execute {
                log.debug("[ClientSettingsController] Updating local settings without notifying the server...")
                clientSettings.update(settings, sendPacket = false)
            }
        }

        C2SPlayChannelEvents.REGISTER.register { handler, sender, client, packets ->
            if (ClientPlayNetworking.canSend(PACKET_SETTINGS_UPDATE_C2S)) {
                log.debug("[ClientSettingsController] Sending packet {} to server", PACKET_SETTINGS_UPDATE_C2S)
                ClientPlayNetworking.send(PACKET_SETTINGS_UPDATE_C2S, clientSettings.getSettings().toPacketBuf())
            } else {
                log.debug("[ClientSettingsController] Not sending {} on join - server unsupported", PACKET_SETTINGS_UPDATE_C2S)
            }
        }
    }

}