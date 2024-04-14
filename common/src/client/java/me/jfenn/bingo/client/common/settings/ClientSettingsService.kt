package me.jfenn.bingo.client.common.settings

import me.jfenn.bingo.client.api.ISessionAccessor
import me.jfenn.bingo.client.common.packet.ClientPacketEvents
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.config.PlayerSettings
import org.slf4j.Logger
import java.util.*

class ClientSettingsService(
    private val log: Logger,
    private val configService: ConfigService,
    sessionAccessor: ISessionAccessor,
    private val packetEvents: ClientPacketEvents,
) {

    private val playerUuid = sessionAccessor.getPlayerUuid()
        ?: UUID.fromString("00000000-0000-0000-0000-000000000000")

    fun getSettings() = configService.playerConfig[playerUuid]

    fun update(settings: PlayerSettings, sendPacket: Boolean = true) {
        configService.setPlayer(playerUuid, settings)

        if (sendPacket) {
            log.debug("[ClientSettingsService] Sending settings update for player")
            packetEvents.sendPlayerSettingsV1.send(settings)
        }
    }

}