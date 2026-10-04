package me.jfenn.bingo.common.commands

import me.jfenn.bingo.api.state.GameState
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.reset.ResetService
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.IExecutors
import me.jfenn.bingo.platform.IServerWorldFactory
import me.jfenn.bingo.platform.commands.ICommandManager

class BingoWorldCommand(
    commandManager: ICommandManager,
    private val text: TextProvider,
) : BingoComponent() {
    init {
        commandManager.register("bingo") {
            literal("world") {
                literal("seed") {
                    requires { hasPermission(Permission.CONFIGURE_GAME) && hasLobby() }

                    executes {
                        val seed = scope.get<IServerWorldFactory>().seed
                        sendFeedback(text.string(
                            StringKey.GameEndSeed,
                            text.bracketedCopyable(seed.toString())
                        ))
                    }

                    long("seed") { seedArg ->
                        requires { hasPermission(Permission.CONFIGURE_GAME) && hasLobby() && hasState(GameState.PREGAME) }
                        executes {
                            val resetService = scope.get<ResetService>()
                            val serverTaskExecutor = scope.get<IExecutors.IServerTaskExecutor>()
                            val options = resetService.preserveOptions(GameState.PREGAME)
                                .copy(worldSeed = getArgument(seedArg))
                            sendFeedback(text.string(StringKey.CommandResetSuccess))
                            serverTaskExecutor.execute {
                                resetService.resetGame(options)
                            }
                        }
                    }
                }
            }
        }
    }
}