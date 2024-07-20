package me.jfenn.bingo.common.commands

import me.jfenn.bingo.api.commands.ICommandManager
import me.jfenn.bingo.api.commands.IExecutionContext
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.config.BingoGoal
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.TierListService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatTitle
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

    private fun IExecutionContext.toggleTierList(listName: String) {
        val options = scope.get<BingoOptions>()
        val events = scope.get<ScopedEvents>()
        val cardService = scope.get<CardService>()

        val isEnabled = options.tierLists.contains(listName)
        when {
            isEnabled && options.tierLists.size > 1 -> options.tierLists -= listName
            else -> options.tierLists += listName
        }

        events.onChangeOptions(Unit)
        cardService.generateCard()
        Text.literal("ℹ  ")
            .append(textProvider.string(StringKey.OptionsTierList))
            .append(": ${options.tierLists.joinToString { it.formatTitle() }}")
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

            literal("tierlist") {
                requires {
                    hasState(GameState.PREGAME, GameState.PLAYING) && hasPermission(Permission.CONFIGURE_GAME)
                }
                for (listName in TierListService.readTierLists()) {
                    literal(listName) {
                        executes {
                            toggleTierList(listName)
                        }
                    }
                }
            }
        }
    }
}