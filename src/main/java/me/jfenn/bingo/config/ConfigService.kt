package me.jfenn.bingo.config

import me.jfenn.bingo.BingoGame
import me.jfenn.bingo.utils.formatTitle
import me.jfenn.bingo.utils.sendInfoMessage
import net.minecraft.server.network.ServerPlayerEntity
import kotlin.reflect.KMutableProperty1

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

    fun setPlayerConfig(
        player: ServerPlayerEntity,
        field: KMutableProperty1<PlayerSettings, Boolean>,
        value: Boolean
    ) {
        val config = getPlayer(player.uuidAsString)
        field.set(config, value)
        setPlayer(player.uuidAsString, config)
        BingoGame.updateSettings(player)
    }

}