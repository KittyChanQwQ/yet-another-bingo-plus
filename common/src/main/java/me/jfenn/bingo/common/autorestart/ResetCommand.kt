package me.jfenn.bingo.common.autorestart

import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.commands.hasPermission
import me.jfenn.bingo.common.commands.hasState
import me.jfenn.bingo.common.ready.ReadyTimerState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.commands.ICommandManager

class ResetCommand(
    commandManager: ICommandManager
) {
    init {
        commandManager.register("restart") {
            requires { hasPermission(Permission.COMMAND_RESTART) && hasState(GameState.POSTGAME) }
            executes { scope.get<ResetService>().resetGame() }

            literal("abort") {
                executes {
                    val text = scope.get<TextProvider>()
                    val timerState = scope.get<ReadyTimerState>()

                    if (timerState.isRunning) {
                        timerState.reset()

                        sendFeedback(text.string(StringKey.CommandRestartAborted))
                    } else {
                        sendFeedback(text.string(StringKey.CommandRestartNotScheduled))
                    }
                }
            }
        }
    }
}