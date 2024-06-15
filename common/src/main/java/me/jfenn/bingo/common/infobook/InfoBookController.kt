package me.jfenn.bingo.common.infobook

import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState

/**
 * Gives the player a tutorial/lobby book when they join the game.
 * This should implement the following conditions:
 * - When isLobbyMode=true, books are persistently available in the lobby
 *   (unless config.lobbyTutorialBook is false)
 * - When isLobbyMode=false, books should never be given to players
 *   unless they run the "/bingo book" command
 */
class InfoBookController(
    events: ScopedEvents,
    private val state: BingoState,
    private val config: BingoConfig,
    private val bookService: InfoBookService,
    private val playerManager: IPlayerManager,
) : BingoComponent() {

    init {
        events.onGameTick {
            if (state.state != GameState.PREGAME)
                return@onGameTick
            if (!config.lobbyTutorialBook)
                return@onGameTick
            if (!state.isLobbyMode)
                return@onGameTick

            for (player in playerManager.getPlayers()) {
                bookService.giveBookItem(player)
            }
        }

        events.onChangePlayerSettings { player ->
            bookService.updateBookItem(playerManager.forPlayer(player))
        }

        events.onPlayerChannelRegister { player ->
            // Update the book to reflect whether the player uses the bingo HUD
            bookService.updateBookItem(playerManager.forPlayer(player))
        }
    }
}