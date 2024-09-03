package me.jfenn.bingo.common.card.tierlist

import me.jfenn.bingo.api.commands.ICommandManager
import me.jfenn.bingo.api.commands.IExecutionContext
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.commands.hasPermission
import me.jfenn.bingo.common.commands.hasState
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.map.CardViewService
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class TierListCommand(
    commandManager: ICommandManager,
    private val tierListState: TierListState,
    private val textProvider: TextProvider,
) : BingoComponent() {

    private fun IExecutionContext.toggleTierList(listName: String) {
        val cardViewService = scope.get<CardViewService>()
        val card = cardViewService.getPlayerCard(player!!)
        when {
            card.options.tierLists.contains(listName) -> setTierList(listName, null)
            else -> setTierList(listName, 1)
        }
    }

    private fun IExecutionContext.setTierList(listName: String, weight: Int?) {
        val events = scope.get<ScopedEvents>()
        val cardService = scope.get<CardService>()
        val tierListService = scope.get<TierListService>()
        val cardViewService = scope.get<CardViewService>()
        val card = cardViewService.getPlayerCard(player!!)

        val isEnabled = card.options.tierLists.contains(listName)
        if (!isEnabled && !tierListState.tierLists.keys.contains(listName)) {
            throw IllegalArgumentException("Tier list '${listName}' is not available.")
        }

        if (weight == null && card.options.tierLists.size <= 1) {
            throw IllegalArgumentException("Tier lists setting must not be empty!")
        }

        when {
            weight != null -> card.options.tierLists += (listName to weight)
            else -> card.options.tierLists -= listName
        }

        events.onChangeOptions(Unit)
        cardService.generateCard()
        Text.literal("ℹ  ")
            .append(textProvider.string(StringKey.OptionsTierList))
            .append(": ")
            .append(tierListService.formatTierLists(card.options))
            .formatted(Formatting.AQUA)
            .also { sendFeedback(it) }
    }

    init {
        commandManager.register("bingo") {
            literal("tierlist") {
                requires {
                    hasState(GameState.PREGAME, GameState.PLAYING) && hasPermission(Permission.CONFIGURE_GAME)
                }
                string("tierlist", { tierListState.tierLists.keys.toList() }) { listNameArg ->
                    executes {
                        val listName = getArgument(listNameArg)
                        toggleTierList(listName)
                    }

                    integer("weight", min = 1) { weightArg ->
                        executes {
                            val listName = getArgument(listNameArg)
                            val weight = getArgument(weightArg)
                            setTierList(listName, weight)
                        }
                    }

                    literal("remove") {
                        executes {
                            val listName = getArgument(listNameArg)
                            setTierList(listName, null)
                        }
                    }
                }
            }
        }
    }
}