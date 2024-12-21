package me.jfenn.bingo.common.game

import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.commands.hasPermission
import me.jfenn.bingo.common.commands.hasState
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.platform.commands.ICommandManager
import me.jfenn.bingo.platform.commands.IExecutionContext
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class GameCommands(
    commandManager: ICommandManager,
) : BingoComponent() {

    private fun IExecutionContext.start(ignoreWarnings: Boolean) {
        val gameService = scope.get<GameService>()
        // Start the game!
        gameService.start(
            ignoreWarnings = ignoreWarnings,
            allowSpectators = ignoreWarnings,
            sendFeedback = { feedback ->
                sendMessage(
                    Text.literal("⚠  ")
                        .append(feedback.copy().formatted(Formatting.ITALIC))
                        .formatted(Formatting.YELLOW),
                )
            }
        )
    }

    init {
        commandManager.register("bingo") {
            literal("start") {
                requires {
                    hasState(GameState.PREGAME) && hasPermission(Permission.CONFIGURE_GAME)
                }

                executes { start(ignoreWarnings = false) }

                literal("ignore_warnings") {
                    executes { start(ignoreWarnings = true) }
                }
            }

            literal("end") {
                requires {
                    hasState(GameState.PLAYING) && hasPermission(Permission.CONFIGURE_GAME)
                }

                executes {
                    val gameService = scope.get<GameService>()
                    val state = scope.get<BingoState>()
                    state.isForfeit = true

                    val player = player
                    val playerName = when {
                        player != null -> player.playerName
                        isConsole -> "RCON"
                        else -> "[unknown]"
                    }

                    gameService.end(GameEndReason.Command(playerName))
                }
            }
        }
    }

    companion object {
        const val START_COMMAND = "/bingo start"
        const val IGNORE_WARNINGS_COMMAND = "/bingo start ignore_warnings"
        const val END_COMMAND = "/bingo end"
    }
}
