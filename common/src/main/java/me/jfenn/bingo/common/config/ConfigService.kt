package me.jfenn.bingo.common.config

import me.jfenn.bingo.common.event.ScopedEvents
import net.minecraft.server.network.ServerPlayerEntity
import kotlin.reflect.KMutableProperty1

object ConfigService {

    val config: BingoConfig by config("bingo/config.json")
    var options: BingoOptions by config("bingo/game-options.json")
    var playerConfig: PlayerConfig by config("bingo/player-settings.json")

    fun writeOptions(options: BingoOptions) {
        this.options = options
    }

    fun getPlayer(uuid: String): PlayerSettings {
        return playerConfig.players[uuid] ?: PlayerSettings()
    }

    fun setPlayer(uuid: String, settings: PlayerSettings) {
        val players = playerConfig.players.toMutableMap()
        players[uuid] = settings
        playerConfig = playerConfig.copy(players = players)
    }

    fun setPlayerConfig(
        events: ScopedEvents,
        player: ServerPlayerEntity,
        field: KMutableProperty1<PlayerSettings, Boolean>,
        value: Boolean
    ) {
        val config = getPlayer(player.uuidAsString)
        field.set(config, value)
        setPlayer(player.uuidAsString, config)

        events.onChangePlayerSettings(player)
    }

}