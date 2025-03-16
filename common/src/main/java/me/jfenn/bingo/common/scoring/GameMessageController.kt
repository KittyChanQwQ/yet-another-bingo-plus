package me.jfenn.bingo.common.scoring

import me.jfenn.bingo.common.event.model.StateChangedEvent
import me.jfenn.bingo.common.event.model.TeamChangedEvent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.platform.IExecutors
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.IPlayerManager
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.model.PlayerEvent

internal class GameMessageController(
    private val state: BingoState,
    private val gameMessageService: GameMessageService,
    private val serverTaskExecutor: IExecutors.IServerTaskExecutor,
    private val playerManager: IPlayerManager,
    eventBus: IEventBus
) {
    private fun scheduleResendMessages(player: IPlayerHandle) {
        for (message in state.gameMessages) {
            if (gameMessageService.isEnabled(player, message)) {
                serverTaskExecutor.execute {
                    gameMessageService.createMessagePacket(player, message)
                        ?.copy(isUpdate = true)
                        ?.let { gameMessageService.sendMessagePacket(player, it) }
                }
            }
        }
    }

    init {
        eventBus.register(PlayerEvent.ChannelRegister) {
            scheduleResendMessages(it.player)
        }

        eventBus.register(TeamChangedEvent) {
            gameMessageService.sendClearMessagesPacket(it.player)
            scheduleResendMessages(it.player)
        }

        eventBus.register(StateChangedEvent) {
            if (it.to == GameState.PREGAME) {
                for (player in playerManager.getPlayers()) {
                    gameMessageService.sendClearMessagesPacket(player)
                }
            }
        }
    }
}