package me.jfenn.bingo.common.controller

import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.options.BingoOptions
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.mixinhandler.GameRuleOverrideHelper
import me.jfenn.bingo.platform.world.IGameRules
import net.minecraft.server.MinecraftServer
import net.minecraft.world.GameRules

/**
 * Controls server setting and gamerule changes:
 * - Hides advancements/death messages in PREGAME
 * - Applies game configuration (pvp, keepinv) when PLAYING
 */
internal class GameRuleController(
    events: ScopedEvents,
    private val server: MinecraftServer,
    private val options: BingoOptions,
    private val state: BingoState,
    private val datapackFunctionService: DatapackFunctionService,
    private val gameRules: IGameRules,
) : BingoComponent() {

    fun readFromServer() {
        options.isPvpEnabled = server.isPvpEnabled
        options.isKeepInventory = server.gameRules.get(GameRules.KEEP_INVENTORY).get()
    }

    fun writeToServer() {
        if (!state.isLobbyMode) return
        server.isPvpEnabled = options.isPvpEnabled
        gameRules.keepInventory.value = options.isKeepInventory
    }

    private fun setPregameRules() {
        // hide spammy messages in the lobby
        GameRuleOverrideHelper.setOverride(gameRules.announceAdvancements, false)
        GameRuleOverrideHelper.setOverride(gameRules.showDeathMessages, false)

        // set up server with the initial config game rules
        writeToServer()
    }

    private fun setCountdownRules() {
        // reset overworld time
        server.overworld.timeOfDay = 0
    }

    private fun setPlayingRules() {
        // revert the lobby gamerule changes
        GameRuleOverrideHelper.setOverride(gameRules.announceAdvancements, null)
        GameRuleOverrideHelper.setOverride(gameRules.showDeathMessages, null)

        // update server to match config choices
        writeToServer()

        // reset overworld time
        server.overworld.timeOfDay = 0
    }

    init {
        events.onStateChange { (from, to) ->
            if (from != to && state.isLobbyMode) {
                when (to) {
                    GameState.PREGAME, GameState.POSTGAME -> setPregameRules()
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
                readFromServer()
            }
        }

        events.onClose {
            GameRuleOverrideHelper.gameRuleOverrides.clear()
        }
    }

}