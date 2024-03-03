package me.jfenn.bingo.common.config

import org.slf4j.Logger

class ConfigService {

    var config: BingoConfig by config("bingo/config.json")
    var options: BingoOptions by config("bingo/game-options.json")
    var optionsDefault: BingoOptions by config("bingo/game-options-default.json")
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

}