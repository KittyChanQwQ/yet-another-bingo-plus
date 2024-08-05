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
    private val datapackFunctionService: DatapackFunctionService,
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

    private fun setPregameRules() {
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

    private fun setCountdownRules() {
        // reset overworld time
        server.overworld.timeOfDay = 0
    }

    private fun setPlayingRules() {
        // revert the lobby gamerule changes
        server.gameRules.get(GameRules.ANNOUNCE_ADVANCEMENTS).set(announceAdvancements, server)
        server.gameRules.get(GameRules.SHOW_DEATH_MESSAGES).set(showDeathMessages, server)

        // update server to match config choices
        server.isPvpEnabled = options.isPvpEnabled
        server.gameRules.get(GameRules.KEEP_INVENTORY).set(options.isKeepInventory, server)

        // reset overworld time
        server.overworld.timeOfDay = 0
    }

    init {
        events.onStateChange { (from, to) ->
            if (from != to && state.isLobbyMode) {
                when (to) {
                    GameState.PREGAME -> setPregameRules()
                    GameState.COUNTDOWN -> setCountdownRules()
                    GameState.PLAYING -> setPlayingRules()
                    else -> {}
                }
            }

            if (from != to) {
                datapackFunctionService.runStateChange(to)
            }
        }

        events.onUpdateTick {
            if (state.state == GameState.PLAYING) {
                // update config with the current game rules (in case /gamerule is used mid-game)
                readFromServer(server, options)
            }
        }
    }

}