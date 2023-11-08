package me.jfenn.bingo.config

object ConfigService {

    var playerConfig: PlayerConfig by config("bingo/players.json")

    fun getPlayer(uuid: String): PlayerSettings {
        return playerConfig.players[uuid] ?: PlayerSettings()
    }

    fun setPlayer(uuid: String, settings: PlayerSettings) {
        val players = playerConfig.players.toMutableMap()
        players[uuid] = settings
        playerConfig = playerConfig.copy(players = players)
    }

}