package me.jfenn.bingo.common.commands

import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.options.BingoCardOptions
import me.jfenn.bingo.common.options.OptionsService
import me.jfenn.bingo.common.options.optionsContext
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.platform.commands.ICommandManager
import me.jfenn.bingo.platform.commands.IExecutionContext
import kotlin.reflect.KMutableProperty1

class BingoModeCommand(
    commandManager: ICommandManager,
) {
    private fun IExecutionContext.toggleCardMode(
        prop: KMutableProperty1<BingoCardOptions, Boolean>,
        value: Boolean? = null,
    ) {
        scope.get<OptionsService>().toggleCardMode(
            ctx = optionsContext,
            prop = prop,
            value = value,
        )
        scope.get<ScopedEvents>().onChangeOptions(Unit)
    }

    private fun IExecutionContext.toggleInfiniteMode() {
        scope.get<OptionsService>().toggleInfiniteMode(optionsContext)
        scope.get<ScopedEvents>().onChangeOptions(Unit)
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
                literal("consume_items") {
                    requires {
                        hasState(GameState.PREGAME, GameState.PLAYING)
                                && hasPermission(Permission.CONFIGURE_GAME)
                                && scope.get<BingoState>().isLobbyMode
                    }
                    executes { toggleCardMode(BingoCardOptions::isConsumeItemsMode) }
                }
                literal("infinite") {
                    executes { toggleInfiniteMode() }
                }
            }
        }
    }
}