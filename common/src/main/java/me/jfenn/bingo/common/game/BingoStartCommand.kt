package me.jfenn.bingo.common.game

import me.jfenn.bingo.api.commands.ICommandManager
import me.jfenn.bingo.api.commands.IExecutionContext
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.commands.hasPermission
import me.jfenn.bingo.common.commands.hasState
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.GameState
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class BingoStartCommand(
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
        }
    }

    companion object {
        const val IGNORE_WARNINGS_COMMAND = "/bingo start ignore_warnings"
    }
}
