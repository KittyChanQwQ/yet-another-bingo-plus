package me.jfenn.bingo.client.common.settings

import me.jfenn.bingo.client.common.packet.ClientPacketEvents
import me.jfenn.bingo.client.platform.event.IClientEvents
import org.slf4j.Logger

internal class ClientSettingsController(
    private val log: Logger,
    private val clientSettings: ClientSettingsService,
    private val packetEvents: ClientPacketEvents,
    clientEvents: IClientEvents,
) {

    init {
        packetEvents.receivePlayerSettingsV1.onPacket { (packet) ->
            log.debug("[ClientSettingsController] Received a settings update (v1) from the server...")
            clientSettings.update(packet, sendPacket = false)
        }

        packetEvents.receivePlayerSettingsV2.onPacket { (packet) ->
            log.debug("[ClientSettingsController] Received a settings update (v2) from the server...")
            clientSettings.update(packet, sendPacket = false)
        }

        clientEvents.onPlayChannelRegister {
            log.debug("[ClientSettingsController] Sending initial settings to the server...")
            val settings = clientSettings.getSettings()
            when {
                packetEvents.sendPlayerSettingsV2.send(settings) -> {}
                packetEvents.sendPlayerSettingsV1.send(settings) -> {}
            }
        }
    }

}