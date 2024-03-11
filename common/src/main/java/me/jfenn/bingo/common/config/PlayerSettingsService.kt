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

    fun setPlayer(
        player: ServerPlayerEntity,
        settings: PlayerSettings,
        seenTutorial: Boolean = false,
    ) {
        if (seenTutorial) {
            settings.seenTutorial = true
        } else {
            val prevConfig = configService.playerConfig[player.uuid]
            settings.seenTutorial = settings.seenTutorial || prevConfig.seenTutorial
        }

        configService.playerConfig.players[player.uuid.toString()] = settings
        configService.writePlayerConfig()
        events.onChangePlayerSettings(player)
    }

}