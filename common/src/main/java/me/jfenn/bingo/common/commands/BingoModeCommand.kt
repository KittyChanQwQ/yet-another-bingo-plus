package me.jfenn.bingo.common.commands

import me.jfenn.bingo.api.commands.ICommandManager
import me.jfenn.bingo.api.commands.IExecutionContext
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.event.ScopedEvents
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
    private fun IExecutionContext.toggleMode(
        prop: KMutableProperty1<BingoOptions, Boolean>,
        value: Boolean? = null,
    ) {
        val options = scope.get<BingoOptions>()
        val newValue = value ?: (!prop.get(options))
        prop.set(options, newValue)

        val events = scope.get<ScopedEvents>()
        events.onChangeOptions(Unit)

        sendInfoFeedback(
            Text.empty()
                .append(text.string(StringKey.OptionsGameMode))
                .append(": ")
                .append(
                    options.formatGameMode()
                        .map { text.string(it) }
                        .joinText()
                )
        )
    }

    init {
        commandManager.register("bingo") {
            literal("mode") {
                requires {
                    hasState(GameState.PREGAME, GameState.PLAYING) && hasPermission(Permission.CONFIGURE_GAME)
                }
                literal("lockout") {
                    executes { toggleMode(BingoOptions::isLockoutMode) }
                }
                literal("inventory") {
                    executes { toggleMode(BingoOptions::isInventoryMode) }
                }
                literal("hidden_items") {
                    executes { toggleMode(BingoOptions::isHiddenItemsMode) }
                }
            }
        }
    }
}