package me.jfenn.bingo.common.config

import kotlinx.serialization.Serializable

@Serializable
data class ServerConfig(
    var isLobbyMode: Boolean = false,
)
