package me.jfenn.bingo.common.config

import kotlinx.serialization.Serializable

@Serializable
data class ClientConfig(
    var cardScale: Float = 1f,
)
