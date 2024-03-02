package me.jfenn.bingo.common.config

object ConfigService {

    var config: BingoConfig by config("bingo/config.json")
    var options: BingoOptions by config("bingo/game-options.json")
    var optionsDefault: BingoOptions by config("bingo/game-options-default.json")
    var playerConfig: PlayerConfig by config("bingo/player-settings.json")

    init {
        this.config.version = MigrationHandler.migrate(this.config.version, this.config)

        // Write the parsed config files on startup
        this.config = this.config.copy()
        this.options = this.options.copy()
        this.optionsDefault = this.optionsDefault.copy()
        this.playerConfig = this.playerConfig.copy()
    }

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