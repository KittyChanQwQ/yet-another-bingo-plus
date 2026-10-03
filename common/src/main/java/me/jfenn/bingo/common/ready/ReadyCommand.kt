package me.jfenn.bingo.common.ready

import me.jfenn.bingo.api.state.GameState
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.commands.hasPermission
import me.jfenn.bingo.common.commands.hasState
import me.jfenn.bingo.common.event.model.OptionsChangedEvent
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.commands.ICommandManager
import me.jfenn.bingo.platform.commands.IExecutionContext
import me.jfenn.bingo.platform.event.IEventBus

class ReadyCommand(
    commandManager: ICommandManager,
    private val text: TextProvider,
    private val eventBus: IEventBus,
) {
    private fun IExecutionContext.voteFor(voteType: VoteType) {
        sendReadyResult(scope.get<ReadyService>().setVote(playerOrThrow, voteType))
    }

    private fun IExecutionContext.sendReadyResult(result: ReadyResult) = when (result) {
        is ReadyResult.ChangedTo -> sendFeedback(result.message)
        else -> error(result.message)
    }

    init {
        commandManager.register("ready") {
            requires {
                scope.get<ReadyService>().isReadyEnabled(playerOrThrow)
            }
            executes {
                sendReadyResult(scope.get<ReadyService>().toggleReady(playerOrThrow))
            }

            literal("next_round") {
                requires {
                    scope.get<ReadyService>().isReadyEnabled(playerOrThrow) && hasState(GameState.POSTGAME)
                }
                executes { voteFor(VoteType.NEXT_ROUND) }
            }

            literal("keep_playing") {
                requires {
                    scope.get<ReadyService>().isReadyEnabled(playerOrThrow) && hasState(GameState.POSTGAME)
                }
                executes { voteFor(VoteType.KEEP_PLAYING) }
            }

            literal("lobby") {
                requires {
                    scope.get<ReadyService>().isReadyEnabled(playerOrThrow) && hasState(GameState.POSTGAME)
                }
                executes { voteFor(VoteType.RETURN_TO_LOBBY) }
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

    companion object {
        const val READY = "/ready"
    }
}