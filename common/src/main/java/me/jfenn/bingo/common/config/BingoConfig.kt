package me.jfenn.bingo.common.config

import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.card.filter.ObjectiveFilterList
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.ready.VoteType
import me.jfenn.bingo.platform.utils.UuidAsString
import java.util.*

@Serializable
data class BingoConfig(
    var version: Int = 0,

    @Description("Provides preset filter settings for the lobby menu and the `/bingo filter` command.")
    var itemFilterPresets: Map<String, ObjectiveFilterList> = ObjectiveFilterList.PRESETS,
    @Description("Provides preset card difficulty settings for the lobby menu and the `/bingo difficulty` command.")
    var difficultyPresets: Map<String, List<Int>> = TierLabel.DIFFICULTY_PRESETS,

    @Description("Adds component tags to all spawn kit items, which prevents them from being scored. (however, this has some drawbacks)")
    val preventScoringSpawnKitItems: Boolean = false,
    @Description("Prevents cards from generating with any of the items that are in the enabled spawn kits.")
    val excludeSpawnKitItemsFromCards: Boolean = true,

    @Description("Turns off sounds that can be caused by other players in the pre-game lobby. This prevents large groups from causing a racket.")
    val preventLobbyChaos: Boolean = false,
    @Description("""
        Gives players a book that explains how to play the game and adjust player settings when in the lobby.
        When this is turned off, players can only obtain the book with the `/bingo book` command.
    """)
    val lobbyTutorialBook: Boolean = true,

    @Description("In survival/SMP mode, the mod gives players a permanent \"memento\" map of each bingo game they complete.")
    val giveMementoInSurvival: Boolean = true,

    @Description("Controls whether all spectating players are given the night vision effect.")
    val nightVisionInSpectator: Boolean = true,
    @Description("Controls whether all players are given the night vision effect when the game is over.")
    val nightVisionInPostgame: Boolean = true,

    @Description("""
        Whether players with the client-side mod can see the bingo card through the HUD.
        If this is false, all players are given a map item the same as vanilla players.
    """)
    var supportClientHud: Boolean = true,

    @Description("""
        The timer that counts down to start the game once all players are on a team.
        Players can vote `/ready` to speed this up, proportionally to the player count. Can be set to `null` to turn off.
    """)
    val startWhenReadySeconds: Int? = 600,
    @Description("Whether the start timer should wait until every player is on a team to start counting down.")
    val startWhenReadyWaitsForTeams: Boolean = true,
    @Description("Whether the start timer should wait until a player votes /ready to start counting down.")
    val startWhenReadyWaitsForFirstVote: Boolean = false,

    @Description("The timer that counts down to start the next game or return to the lobby when a game is over. Can be set to `null` to turn off.")
    val nextRoundWhenReadySeconds: Int? = 600,
    @Description("Whether the \"Next Round\" timer should wait until at least one player has voted `/ready` to start counting down.")
    val nextRoundWhenReadyWaitsForFirstVote: Boolean = true,

    @Description("The number of minutes to extend a match by if the 'Extend by ...' (KEEP_PLAYING) option is picked.")
    val extendRoundTimeLimitByMinutes: Int = 15,

    @Description("The default option to choose after the game ends. This can be `NEXT_ROUND`, `KEEP_PLAYING`, or `RETURN_TO_LOBBY`.")
    val defaultPostGameReadyOption: VoteType = VoteType.NEXT_ROUND,

    @Description("""
        Whether to keep players on the same teams when returning to the lobby after a game has finished.
        By default, all teams are removed, and players must choose their teams again.
    """)
    val keepTeamsAfterRoundEnds: Boolean = false,

    @Description("Amount of ticks to wait after teleporting players into the world before starting the countdown.")
    val countdownDelayTicks: Int = 100,
    @Description("Number of seconds to count down before starting the game.")
    val countdownSeconds: Int = 3,

    @Description("Automatically resets the server if all players disconnect after a game has ended.")
    val resetWhenEveryoneDisconnects: Boolean = true,

    val chat: ChatConfig = ChatConfig(),

    @Description("""
        Provides a [JDBC database URL](https://docs.oracle.com/javase/tutorial/jdbc/basics/connecting.html), username, and password.
        Currently, only SQLite is supported. By default, this uses a `bingo.db` file in the mod's config directory.
    """)
    val databaseUrl: String? = null,
    val databaseUser: String? = null,
    val databasePass: String? = null,

    @Description("""
        Whether to sync player statistics (playtime, win/loss, items scored) with any connected clients.
        By default, your bingo statistics include all games you've played across any server or singleplayer world.
        Turning this off will only keep statistics for games that are played on your server.
    """)
    var syncStats: Boolean = true,
    @Description("""
        Provides a unique ID for each host that can be used to determine which server a game was played on, for calculating the "Best Time" statistics.
    """)
    val statsHostId: UuidAsString = UUID.randomUUID(),

    @Description("""
        Client-side settings can all be edited in the settings through [ModMenu](https://modrinth.com/mod/modmenu).
        You don't need to change these manually.
    """)
    val client: ClientConfig = ClientConfig(),

    // only applies to dedicated server
    val server: ServerConfig = ServerConfig(),
)
