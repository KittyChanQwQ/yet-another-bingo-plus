package me.jfenn.bingo.integrations

import me.jfenn.bingo.state.GameState
import net.minecraft.world.GameRules

/**
 * Controls server setting and gamerule changes:
 * - Hides advancements/death messages in PREGAME
 * - Applies game configuration (pvp, keepinv) when PLAYING
 */
object GameRuleController {

    fun init() {
        GameState.PREGAME.onEnter {
            // remove overworld spawn tickets
            server.overworld.chunkManager.removePersistentTickets()

            // hide spammy messages in the lobby
            server.gameRules.get(GameRules.ANNOUNCE_ADVANCEMENTS).set(false, server)
            server.gameRules.get(GameRules.SHOW_DEATH_MESSAGES).set(false, server)
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