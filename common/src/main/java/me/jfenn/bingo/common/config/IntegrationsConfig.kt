package me.jfenn.bingo.common.config

import kotlinx.serialization.Serializable

@Serializable
data class IntegrationsConfig(
    val vanish: Boolean = true,
)
