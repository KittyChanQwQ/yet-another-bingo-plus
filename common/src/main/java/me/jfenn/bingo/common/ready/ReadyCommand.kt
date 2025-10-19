package me.jfenn.bingo.common.ready

import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.commands.hasPermission
import me.jfenn.bingo.common.event.model.OptionsChangedEvent
import me.jfenn.bingo.common.team.JoinCommand
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.commands.ICommandManager
import me.jfenn.bingo.platform.event.IEventBus

class ReadyCommand(
    commandManager: ICommandManager,
    private val text: TextProvider,
    private val eventBus: IEventBus,
) {
    init {
        commandManager.register("ready") {
            requires {
                scope.get<ReadyService>().isReadyEnabled(playerOrThrow)
            }
            executes {
                val readyService = scope.get<ReadyService>()
                if (readyService.canUseReady(playerOrThrow)) {
                    val isReady = scope.get<ReadyService>().toggleReady(playerOrThrow)
                    sendFeedback(text.string(
                        if (isReady) StringKey.CommandReadyTrue else StringKey.CommandReadyFalse,
                        playerOrThrow.playerName
                    ))
                } else {
                    error(text.string(StringKey.CommandReadyNotOnATeam, "${JoinCommand.JOIN_COMMAND} ${JoinCommand.SPECTATORS}"))
                }
            }

            literal("cancel") {
                requires { hasPermission(Permission.CONFIGURE_GAME) }
                executes {
                    val timerState = scope.get<ReadyTimerState>()

                    if (timerState.isRunning) {
                        timerState.reset()
                        timerState.isCancelled = true
                        // call onChangeOptions to refresh the player list
                        eventBus.emit(OptionsChangedEvent, Unit)
                    }

                    sendFeedback(text.string(StringKey.CommandReadyCancelled))
                }
            }
        }
    }
}