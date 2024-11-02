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

    private fun IExecutionContext.setTierList(listName: String?) {
        val events = scope.get<ScopedEvents>()
        val cardService = scope.get<CardService>()
        val tierListService = scope.get<TierListService>()
        val cardViewService = scope.get<CardViewService>()
        val card = cardViewService.getPlayerCard(player!!)

        if (listName != null && !tierListState.tierLists.keys.contains(listName)) {
            throw IllegalArgumentException("Tier list '${listName}' is not available.")
        }

        card.options.tierList = listName

        events.onChangeOptions(Unit)
        cardService.generateCard()
        Text.literal("ℹ  ")
            .append(textProvider.string(StringKey.OptionsTierList))
            .append(": ")
            .append(tierListService.formatTierList(card.options) ?: Text.literal("null"))
            .formatted(Formatting.AQUA)
            .also { sendFeedback(it) }
    }

    init {
        commandManager.register("bingo") {
            literal("tierlist") {
                requires {
                    hasState(GameState.PREGAME, GameState.PLAYING) && hasPermission(Permission.CONFIGURE_GAME)
                }
                literal("all") {
                    executes {
                        setTierList(null)
                    }
                }
                string("tierlist", { tierListState.tierLists.keys.toList() }) { listNameArg ->
                    executes {
                        val listName = getArgument(listNameArg)
                        setTierList(listName)
                    }
                }
            }
        }
    }
}