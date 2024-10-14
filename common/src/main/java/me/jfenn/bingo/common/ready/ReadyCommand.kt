package me.jfenn.bingo.common.ready

import me.jfenn.bingo.api.commands.ICommandManager
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.commands.hasPermission
import me.jfenn.bingo.common.commands.hasState
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState

class ReadyCommand(
    private val readyState: ReadyTimerState,
    private val config: BingoConfig,
    commandManager: ICommandManager,
) {
    init {
        commandManager.register("ready") {
            requires {
                val isLobbyMode = scope.get<BingoState>().isLobbyMode
                val isPregameCountdown = hasState(GameState.PREGAME) && config.startWhenReadySeconds != null
                val isPostgameCountdown = hasState(GameState.POSTGAME) && config.nextRoundWhenReadySeconds != null
                isLobbyMode && (isPregameCountdown || isPostgameCountdown) && hasPermission(Permission.COMMAND_READY)
            }
            executes {
                val uuid = player!!.uuid
                readyState.setReady(uuid, !readyState.isReady(uuid))
            }
        }
    }
}