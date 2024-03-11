package me.jfenn.bingo.common.config

import java.util.*

class ConfigService {

    var config: BingoConfig by config("bingo/config.json")
    var options: BingoOptions by config("bingo/game-options.json")
    var optionsDefault: BingoOptions by config("bingo/game-options-default.json")
    var playerConfig: PlayerConfig by config("bingo/player-settings.json")

    fun writeConfig(config: BingoConfig) {
        this.config = config
    }

    fun writeOptions(options: BingoOptions) {
        this.options = options
    }

    fun writePlayerConfig() {
        this.playerConfig = playerConfig
    }

    fun setPlayer(uuid: UUID, settings: PlayerSettings) {
        playerConfig.players[uuid.toString()] = settings
        writePlayerConfig()
    }

}