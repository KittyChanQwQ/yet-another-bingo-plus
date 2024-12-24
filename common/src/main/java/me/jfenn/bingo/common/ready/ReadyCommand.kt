package me.jfenn.bingo.common.ready

import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.commands.hasPermission
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.commands.ICommandManager

class ReadyCommand(
    commandManager: ICommandManager,
    private val text: TextProvider,
) {
    init {
        commandManager.register("ready") {
            requires {
                scope.get<ReadyService>().canUseReady(playerOrThrow)
            }
            executes {
                scope.get<ReadyService>().toggleReady(playerOrThrow)
            }

            literal("cancel") {
                requires { hasPermission(Permission.CONFIGURE_GAME) }
                executes {
                    val timerState = scope.get<ReadyTimerState>()

                    if (timerState.isRunning) {
                        timerState.reset()
                        // call onChangeOptions to refresh the player list
                        scope.get<ScopedEvents>().onChangeOptions.invoke(Unit)
                    }

                    sendFeedback(text.string(StringKey.CommandReadyCancelled))
                }
            }
        }
    }
}