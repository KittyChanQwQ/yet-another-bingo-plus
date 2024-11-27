package me.jfenn.bingo.common.commands

import me.jfenn.bingo.platform.commands.ICommandManager
import me.jfenn.bingo.platform.commands.IExecutionContext
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.config.BingoCardOptions
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.BingoWinCondition
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.map.CardViewService
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.joinText
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text
import kotlin.reflect.KMutableProperty1

class BingoModeCommand(
    commandManager: ICommandManager,
    private val text: TextProvider,
) {
    private fun IExecutionContext.sendModeFeedback(
        card: BingoCard = scope.get<CardViewService>().getPlayerCard(player!!)
    ) {
        val options = scope.get<BingoOptions>()

        sendInfoFeedback(
            Text.empty()
                .append(text.string(StringKey.OptionsGameMode))
                .append(": ")
                .append(
                    options.formatGameMode(card)
                        .map { text.string(it) }
                        .joinText()
                )
        )
    }

    private fun IExecutionContext.toggleCardMode(
        prop: KMutableProperty1<BingoCardOptions, Boolean>,
        value: Boolean? = null,
    ) {
        val cardViewService = scope.get<CardViewService>()
        val card = cardViewService.getPlayerCard(player!!)

        val newValue = value ?: (!prop.get(card.options))
        prop.set(card.options, newValue)

        val events = scope.get<ScopedEvents>()
        events.onChangeOptions(Unit)

        sendModeFeedback(card)
    }

    private fun IExecutionContext.toggleInfiniteMode() {
        val options = scope.get<BingoOptions>()
        options.winCondition = when (options.winCondition) {
            is BingoWinCondition.Cards -> BingoWinCondition.Infinite
            is BingoWinCondition.Infinite -> BingoWinCondition.Cards(1)
        }
    }

    init {
        commandManager.register("bingo") {
            literal("mode") {
                requires {
                    hasState(GameState.PREGAME, GameState.PLAYING) && hasPermission(Permission.CONFIGURE_GAME)
                }
                literal("lockout") {
                    executes { toggleCardMode(BingoCardOptions::isLockoutMode) }
                }
                literal("inventory") {
                    executes { toggleCardMode(BingoCardOptions::isInventoryMode) }
                }
                literal("hidden_items") {
                    executes { toggleCardMode(BingoCardOptions::isHiddenItemsMode) }
                }
                literal("infinite") {
                    executes { toggleInfiniteMode() }
                }
            }
        }
    }
}