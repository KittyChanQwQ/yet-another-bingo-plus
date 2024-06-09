package me.jfenn.bingo.common.game

import me.jfenn.bingo.api.commands.ICommandManager
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.commands.hasPermission
import me.jfenn.bingo.common.commands.hasState
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class BingoStartCommand(
    commandManager: ICommandManager,
    private val textProvider: TextProvider,
) : BingoComponent() {
    init {
        commandManager.register("bingo") {
            literal("start") {
                requires {
                    hasState(GameState.PREGAME) && hasPermission(Permission.CONFIGURE_GAME)
                }
                executes {
                    val gameService = scope.get<GameService>()
                    // Start the game!
                    gameService.start(
                        sendFeedback = { feedback ->
                            sendMessage(
                                Text.literal("⚠  ")
                                    .append(textProvider.string(feedback).formatted(Formatting.ITALIC))
                                    .formatted(Formatting.YELLOW),
                            )
                        }
                    )
                }
            }
        }
    }
}
