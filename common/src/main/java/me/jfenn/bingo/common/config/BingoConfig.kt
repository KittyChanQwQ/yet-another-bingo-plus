package me.jfenn.bingo.common.config

import kotlinx.serialization.Serializable

@Serializable
data class BingoConfig(
    var version: Int = 0,

    // adds BINGO_IGNORE to spawn kit items, preventing them from being scored
    val preventScoringSpawnKitItems: Boolean = false,
    // excludes the active spawn kit items from item tiers when generating a card
    val excludeSpawnKitItemsFromCards: Boolean = true,

    // stops sounds from being sent to other players in the lobby
    val preventLobbyChaos: Boolean = false,

    val nightVisionInSpectator: Boolean = true,
    val nightVisionInPostgame: Boolean = true,

    // whether the server should allow connecting clients to use the bingo HUD
    // instead of the vanilla map/card behavior
    val supportClientHud: Boolean = true,

    // shuts down the server once all players log out after a game
    val autoShutDownInPostgame: Boolean = true,
    // shuts down the server faster, at the risk of bugs or data corruption
    val unsafeSkipWorldClose: Boolean = false,

    val integrations: IntegrationsConfig = IntegrationsConfig(),
)
