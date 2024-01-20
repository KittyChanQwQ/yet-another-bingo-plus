package me.jfenn.bingo.common.config

import kotlinx.serialization.Serializable

@Serializable
data class BingoConfig(
    val overwriteOptions: Boolean = true,

    // adds BINGO_IGNORE to spawn kit items, preventing them from being scored
    val preventScoringSpawnKitItems: Boolean = false,
    // excludes the active spawn kit items from item tiers when generating a card
    val excludeSpawnKitItemsFromCards: Boolean = true,

    // stops sounds from being sent to other players in the lobby
    val preventLobbyChaos: Boolean = false,

    // shuts down the server once all players log out after a game
    val autoShutDownInPostgame: Boolean = true,
    // shuts down the server faster, at the risk of bugs or data corruption
    val unsafeSkipWorldClose: Boolean = false,
)
