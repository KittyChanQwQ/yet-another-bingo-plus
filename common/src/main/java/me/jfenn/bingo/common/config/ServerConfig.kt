package me.jfenn.bingo.common.config

import kotlinx.serialization.Serializable

@Serializable
data class ServerConfig(
    var isLobbyMode: Boolean = false,
    // fixed world seed to apply when resetting
    val fixedSeed: Long = 0,
    val fixedSeedEnabled: Boolean = false,
    // world template location, relative to config/yet-another-minecraft-bingo
    val templateDir: String = "template",
    val templateEnabled: Boolean = true,
    val preloadViewDistance: Int = 4,
    val filesToReset: List<String> = listOf(
        "advancements",
        "data/bingo.dat",
        "data/idcounts.dat",
        "data/map_*.dat",
        "data/raids.dat",
        "data/random_sequences.dat",
        "DIM*",
        "dimensions",
        "entities",
        "playerdata",
        "poi",
        "region",
        "stats",
        "level.dat",
        "level.dat_old",
    ),
    val defaultPlayerSettings: PlayerSettings = PlayerSettings(),
)
