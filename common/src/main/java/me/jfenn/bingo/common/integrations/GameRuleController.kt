package me.jfenn.bingo.common.integrations

import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import net.minecraft.server.MinecraftServer
import net.minecraft.world.GameRules

/**
 * Controls server setting and gamerule changes:
 * - Hides advancements/death messages in PREGAME
 * - Applies game configuration (pvp, keepinv) when PLAYING
 */
class GameRuleController(
    events: ScopedEvents,
    private val server: MinecraftServer,
    private val options: BingoOptions,
    private val state: BingoState,
) {

    fun readFromServer(server: MinecraftServer, options: BingoOptions) {
        options.isPvpEnabled = server.isPvpEnabled
        options.isKeepInventory = server.gameRules.get(GameRules.KEEP_INVENTORY).get()
    }

    fun writeToServer(server: MinecraftServer, options: BingoOptions) {
        server.isPvpEnabled = options.isPvpEnabled
        server.gameRules.get(GameRules.KEEP_INVENTORY).set(options.isKeepInventory, server)
    }

    init {
        events.onEnter(GameState.PREGAME) {
            // remove overworld spawn tickets
            server.overworld.chunkManager.removePersistentTickets()

            // hide spammy messages in the lobby
            server.gameRules.get(GameRules.ANNOUNCE_ADVANCEMENTS).set(false, server)
            server.gameRules.get(GameRules.SHOW_DEATH_MESSAGES).set(false, server)

            // set up server with the initial config game rules
            writeToServer(server, options)
        }

        events.onUpdateTick {
            if (state.state == GameState.PLAYING) {
                // update config with the current game rules (in case /gamerule is used mid-game)
                readFromServer(server, options)
            }
        }

        events.onEnter(GameState.PLAYING) { prevState ->
            // revert the lobby gamerule changes
            server.gameRules.get(GameRules.ANNOUNCE_ADVANCEMENTS).set(true, server)
            server.gameRules.get(GameRules.SHOW_DEATH_MESSAGES).set(true, server)

            // update server to match config choices
            server.isPvpEnabled = options.isPvpEnabled
            server.gameRules.get(GameRules.KEEP_INVENTORY).set(options.isKeepInventory, server)

            if (prevState == GameState.PREGAME) {
                // reset overworld time
                server.overworld.timeOfDay = 0
            }
        }
    }

}