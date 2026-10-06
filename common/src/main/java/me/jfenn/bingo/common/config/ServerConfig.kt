package me.jfenn.bingo.common.config

import kotlinx.serialization.Serializable

@Serializable
data class ServerConfig(
    @Description("""
        Whether the bingo lobby (and other destructive game options) are enabled.
        **Use the `/bingo lobby` command to read the warnings before turning this on. This will delete your world folder!**
    """)
    var isLobbyMode: Boolean = false,
    @Description("""
        Specifies a fixed seed to apply to each new world in lobby mode.
        If this is enabled, each round will start on this seed, unless it is manually changed with `/bingo world seed`. 
    """)
    val fixedSeed: Long = 0,
    val fixedSeedEnabled: Boolean = false,
    @Description("""
        Specifies a template world (within the config folder) to copy for each new round.
        The world should be copied so that "level.dat" is directly within this folder, i.e. `config/yet-another-minecraft-bingo/template/level.dat`.
        This can be used to pregenerate a large world to run a tournament or event, to avoid world generation lag.
        If enabled, this takes precedence over the fixed seed; the seed from the template will be applied instead.
    """)
    val templateDir: String = "template",
    val templateEnabled: Boolean = true,
    @Description("""
        Lobby-mode only: Sets the view distance that should be preloaded around each player's spawn before starting the game.
        Increasing this can make games much slower to start.
    """)
    val preloadViewDistance: Int = 4,
    @Description("The default per-player settings. Players can change these with `/bingoprefs`, or in the client mod settings screen.")
    val defaultPlayerSettings: PlayerSettings = PlayerSettings(),
)
