package me.jfenn.bingo.common.config

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class BingoConfig(
    var version: Int = 0,

    // adds BINGO_IGNORE to spawn kit items, preventing them from being scored
    val preventScoringSpawnKitItems: Boolean = false,
    // excludes the active spawn kit items from item tiers when generating a card
    val excludeSpawnKitItemsFromCards: Boolean = true,

    // stops sounds from being sent to other players in the lobby
    val preventLobbyChaos: Boolean = false,
    // gives players a tutorial/settings book when they join the lobby
    val lobbyTutorialBook: Boolean = true,

    val nightVisionInSpectator: Boolean = true,
    val nightVisionInPostgame: Boolean = true,

    // whether the server should allow connecting clients to use the bingo HUD
    // instead of the vanilla map/card behavior
    var supportClientHud: Boolean = true,

    // shuts down the server once all players log out after a game
    @JsonNames("autoShutDownInPostgame")
    val shutDownWhenEveryoneDisconnects: Boolean = true,
    val shutDownWhenGameOverSeconds: Int? = null,
    // shuts down the server faster, at the risk of bugs or data corruption
    val unsafeSkipWorldClose: Boolean = false,

    val integrations: IntegrationsConfig = IntegrationsConfig(),

    val databaseUrl: String? = null,
    val databaseUser: String? = null,
    val databasePass: String? = null,

    // whether to sync game stats with any connected servers/clients
    var syncStats: Boolean = true,

    // only applies to client
    val client: ClientConfig = ClientConfig(),

    // only applies to dedicated server
    val server: ServerConfig = ServerConfig(),
)
