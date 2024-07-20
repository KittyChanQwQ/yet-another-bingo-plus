package me.jfenn.bingo.common.commands

import me.jfenn.bingo.api.commands.ICommandManager
import me.jfenn.bingo.api.commands.IExecutionContext
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.config.BingoGoal
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class BingoOptionsCommands(
    commandManager: ICommandManager,
    private val textProvider: TextProvider,
) : BingoComponent() {

    private fun IExecutionContext.setGoal(goal: BingoGoal) {
        val state = scope.get<BingoState>()
        state.options.cardGoal = goal

        val events = scope.get<ScopedEvents>()
        events.onChangeOptions(Unit)

        // Send a message that the goal was changed
        Text.literal("ℹ  ")
            .append(textProvider.string(StringKey.ScoreboardGoal, goal.format(textProvider)))
            .formatted(Formatting.AQUA)
            .also { sendFeedback(it) }
    }

    init {
        commandManager.register("bingo") {
            literal("goal") {
                requires {
                    hasState(GameState.PREGAME, GameState.PLAYING) && hasPermission(Permission.CONFIGURE_GAME)
                }
                integer("count", min = 1, max = 25) { countArg ->
                    literal("items") {
                        executes {
                            val count = getArgument(countArg).coerceIn(1, BingoGoal.MAX_ITEMS)
                            setGoal(BingoGoal.Items(count))
                        }
                    }
                    literal("lines") {
                        executes {
                            val count = getArgument(countArg).coerceIn(1, BingoGoal.MAX_LINES)
                            setGoal(BingoGoal.Lines(count))
                        }
                    }
                }
            }
        }
    }
}