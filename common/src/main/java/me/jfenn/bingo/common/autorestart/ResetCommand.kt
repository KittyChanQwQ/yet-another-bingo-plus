package me.jfenn.bingo.common.autorestart

import me.jfenn.bingo.api.commands.ICommandManager
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.commands.hasPermission
import me.jfenn.bingo.common.commands.hasState
import me.jfenn.bingo.common.state.GameState

class ResetCommand(
    commandManager: ICommandManager
) {
    init {
        commandManager.register("bingo") {
            literal("reset") {
                requires { hasPermission(Permission.COMMAND_RESTART) && hasState(GameState.POSTGAME) }
                executes { scope.get<ResetService>().resetGame() }
            }
        }
    }
}