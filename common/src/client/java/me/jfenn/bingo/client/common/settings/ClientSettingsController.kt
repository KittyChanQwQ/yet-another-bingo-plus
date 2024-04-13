package me.jfenn.bingo.client.common.settings

import me.jfenn.bingo.client.common.packet.ClientPacketManager
import me.jfenn.bingo.common.config.PlayerSettings
import net.fabricmc.fabric.api.client.networking.v1.C2SPlayChannelEvents
import org.slf4j.Logger

class ClientSettingsController(
    private val log: Logger,
    private val clientSettings: ClientSettingsService,
    private val packetManager: ClientPacketManager,
) {

    init {
        packetManager.registerGlobalReceiver(PlayerSettings.V1_S2C) { client, handler, packet, response ->
            log.debug("[ClientSettingsController] Received a settings update from the server...")
            clientSettings.update(packet, sendPacket = false)
        }

        C2SPlayChannelEvents.REGISTER.register { handler, sender, client, packets ->
            log.debug("[ClientSettingsController] Sending initial settings to the server...")
            packetManager.sendPacketC2S(PlayerSettings.V1_C2S, value = clientSettings.getSettings())
        }
    }

}