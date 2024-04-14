package me.jfenn.bingo.client.common.settings

import me.jfenn.bingo.client.common.packet.ClientPacketEvents
import net.fabricmc.fabric.api.client.networking.v1.C2SPlayChannelEvents
import org.slf4j.Logger

class ClientSettingsController(
    private val log: Logger,
    private val clientSettings: ClientSettingsService,
    private val packetEvents: ClientPacketEvents,
) {

    init {
        packetEvents.receivePlayerSettingsV1.onPacket { (packet) ->
            log.debug("[ClientSettingsController] Received a settings update from the server...")
            clientSettings.update(packet, sendPacket = false)
        }

        C2SPlayChannelEvents.REGISTER.register { _, _, _, _ ->
            log.debug("[ClientSettingsController] Sending initial settings to the server...")
            packetEvents.sendPlayerSettingsV1.send(clientSettings.getSettings())
        }
    }

}