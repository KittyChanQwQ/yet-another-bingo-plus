package me.jfenn.bingo.common.lobby

import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.game.GameResetEvent

internal class LobbyWorldController(
    private val lobbyWorldService: LobbyWorldService,
    state: BingoState,
    eventBus: IEventBus,
) {
    init {
        if (state.isLobbyMode) {
            lobbyWorldService.prepareLobbyFiles()
        }

        eventBus.register(GameResetEvent.Copy) {
            lobbyWorldService.prepareLobbyFiles()
        }
    }
}