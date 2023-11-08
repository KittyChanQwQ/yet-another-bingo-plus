package me.jfenn.bingo.config

import kotlinx.serialization.Serializable

@Serializable
data class PlayerConfig(
    val players: Map<String, PlayerSettings> = mapOf()
) {

    operator fun get(uuid: String): PlayerSettings {
        return players[uuid] ?: PlayerSettings()
    }

}
