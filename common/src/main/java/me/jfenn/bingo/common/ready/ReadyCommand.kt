package me.jfenn.bingo.common.ready

import me.jfenn.bingo.api.commands.ICommandManager
import me.jfenn.bingo.common.commands.hasState
import me.jfenn.bingo.common.state.GameState

class ReadyCommand(
    private val readyState: ReadyTimerState,
    commandManager: ICommandManager,
) {
    init {
        commandManager.register("ready") {
            requires {
                hasState(GameState.PREGAME, GameState.POSTGAME)
            }
            executes {
                val uuid = player!!.uuid
                readyState.setReady(uuid, !readyState.isReady(uuid))
            }
        }
    }
}