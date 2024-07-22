package me.jfenn.bingo.common.controller

import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
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
) : BingoComponent() {

    fun readFromServer(server: MinecraftServer, options: BingoOptions) {
        options.isPvpEnabled = server.isPvpEnabled
        options.isKeepInventory = server.gameRules.get(GameRules.KEEP_INVENTORY).get()
    }

    fun writeToServer(server: MinecraftServer, options: BingoOptions) {
        if (!state.isLobbyMode) return
        server.isPvpEnabled = options.isPvpEnabled
        server.gameRules.get(GameRules.KEEP_INVENTORY).set(options.isKeepInventory, server)
    }

    private var announceAdvancements = false
    private var showDeathMessages = false

    init {
        events.onEnter(GameState.PREGAME) {
            if (!state.isLobbyMode) return@onEnter

            // remove overworld spawn tickets
            server.overworld.chunkManager.removePersistentTickets()

            // hide spammy messages in the lobby
            announceAdvancements = server.gameRules.get(GameRules.ANNOUNCE_ADVANCEMENTS).get()
            server.gameRules.get(GameRules.ANNOUNCE_ADVANCEMENTS).set(false, server)

            showDeathMessages = server.gameRules.get(GameRules.SHOW_DEATH_MESSAGES).get()
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

        events.onEnter(GameState.COUNTDOWN) {
            if (!state.isLobbyMode) return@onEnter

            // reset overworld time
            server.overworld.timeOfDay = 0
        }

        events.onEnter(GameState.PLAYING) { prevState ->
            if (!state.isLobbyMode) return@onEnter

            // revert the lobby gamerule changes
            server.gameRules.get(GameRules.ANNOUNCE_ADVANCEMENTS).set(announceAdvancements, server)
            server.gameRules.get(GameRules.SHOW_DEATH_MESSAGES).set(showDeathMessages, server)

            // update server to match config choices
            server.isPvpEnabled = options.isPvpEnabled
            server.gameRules.get(GameRules.KEEP_INVENTORY).set(options.isKeepInventory, server)

            if (prevState != GameState.PLAYING) {
                // reset overworld time
                server.overworld.timeOfDay = 0
            }
        }
    }

}