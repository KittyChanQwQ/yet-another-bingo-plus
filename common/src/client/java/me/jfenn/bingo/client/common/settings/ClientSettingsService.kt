package me.jfenn.bingo.client.common.settings

import me.jfenn.bingo.client.api.ISessionAccessor
import me.jfenn.bingo.common.PACKET_SETTINGS_UPDATE_C2S
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.config.PlayerSettings
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import org.slf4j.Logger
import java.util.*

class ClientSettingsService(
    private val log: Logger,
    private val configService: ConfigService,
    sessionAccessor: ISessionAccessor,
) {

    private val playerUuid = sessionAccessor.getPlayerUuid()
        ?: UUID.fromString("00000000-0000-0000-0000-000000000000")

    fun getSettings() = configService.playerConfig[playerUuid]

    fun update(settings: PlayerSettings, sendPacket: Boolean = true) {
        configService.setPlayer(playerUuid, settings)

        if (sendPacket) {
            if (ClientPlayNetworking.canSend(PACKET_SETTINGS_UPDATE_C2S)) {
                log.debug("[ClientSettingsService] Sending {} packet for player", PACKET_SETTINGS_UPDATE_C2S)
                ClientPlayNetworking.send(PACKET_SETTINGS_UPDATE_C2S, settings.toPacketBuf())
            } else {
                log.debug(
                    "[ClientSettingsService] Not sending an update packet - {} is unsupported",
                    PACKET_SETTINGS_UPDATE_C2S
                )
            }
        }
    }

}