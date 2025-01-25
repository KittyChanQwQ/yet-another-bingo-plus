package me.jfenn.bingo.common.config

import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.options.BingoOptions
import java.util.*

class ConfigService {

    var config: BingoConfig by config("$MOD_ID_BINGO/config.json")
    var options: BingoOptions by config("$MOD_ID_BINGO/game-options.json")
    var optionsDefault: BingoOptions by config("$MOD_ID_BINGO/game-options-default.json")
    var playerConfig: PlayerConfig by config("$MOD_ID_BINGO/player-settings.json")
    var files: TrackedFiles by config("$MOD_ID_BINGO/files.json")

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