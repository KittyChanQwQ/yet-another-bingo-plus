package me.jfenn.bingo.common.config

import kotlinx.serialization.Serializable

@Serializable
data class IntegrationsConfig(
    val vanish: Boolean = true,
    val chunky: Boolean = true,
    val chunkyWorlds: Map<String, Int> = mapOf(
        "minecraft:overworld" to 8000,
        "minecraft:the_nether" to 1000,
    )
)