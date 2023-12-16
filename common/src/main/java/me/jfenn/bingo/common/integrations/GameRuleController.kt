package me.jfenn.bingo.common.integrations

import me.jfenn.bingo.common.Bingo
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.state.GameState
import net.minecraft.server.MinecraftServer
import net.minecraft.world.GameRules

/**
 * Controls server setting and gamerule changes:
 * - Hides advancements/death messages in PREGAME
 * - Applies game configuration (pvp, keepinv) when PLAYING
 */
object GameRuleController {

    fun readFromServer(server: MinecraftServer, config: BingoConfig) {
        config.isPvpEnabled = server.isPvpEnabled
        config.isKeepInventory = server.gameRules.get(GameRules.KEEP_INVENTORY).get()
    }

    fun writeToServer(server: MinecraftServer, config: BingoConfig) {
        server.isPvpEnabled = config.isPvpEnabled
        server.gameRules.get(GameRules.KEEP_INVENTORY).set(config.isKeepInventory, server)
    }

    fun init() {
        GameState.PREGAME.onEnter {
            // remove overworld spawn tickets
            server.overworld.chunkManager.removePersistentTickets()

            // hide spammy messages in the lobby
            server.gameRules.get(GameRules.ANNOUNCE_ADVANCEMENTS).set(false, server)
            server.gameRules.get(GameRules.SHOW_DEATH_MESSAGES).set(false, server)

            // set up server with the initial config game rules
            writeToServer(server, config)
        }

        Bingo.onUpdateTick {
            if (state.state == GameState.PLAYING) {
                // update config with the current game rules (in case /gamerule is used mid-game)
                readFromServer(server, config)
            }
        }

        GameState.PLAYING.onEnter { prevState ->
            // revert the lobby gamerule changes
            server.gameRules.get(GameRules.ANNOUNCE_ADVANCEMENTS).set(true, server)
            server.gameRules.get(GameRules.SHOW_DEATH_MESSAGES).set(true, server)

            // update server to match config choices
            server.isPvpEnabled = config.isPvpEnabled
            server.gameRules.get(GameRules.KEEP_INVENTORY).set(config.isKeepInventory, server)

            if (prevState == GameState.PREGAME) {
                // reset overworld time
                server.overworld.timeOfDay = 0
            }
        }
    }

}