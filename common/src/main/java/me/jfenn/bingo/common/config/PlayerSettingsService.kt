package me.jfenn.bingo.common.config

import me.jfenn.bingo.common.event.ScopedEvents
import net.minecraft.server.network.ServerPlayerEntity

class PlayerSettingsService(
    private val configService: ConfigService,
    private val events: ScopedEvents,
) {

    fun getPlayer(player: ServerPlayerEntity): PlayerSettings {
        return configService.playerConfig[player.uuid]
    }

    fun setPlayer(player: ServerPlayerEntity, settings: PlayerSettings) {
        configService.playerConfig.players[player.uuid.toString()] = settings
        configService.writePlayerConfig()
        events.onChangePlayerSettings(player)
    }

}