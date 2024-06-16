package me.jfenn.bingo.common.commands

import me.jfenn.bingo.api.commands.ICommandManager
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class PreviewCardCommand(
    commandManager: ICommandManager,
    private val text: TextProvider,
) {
    init {
        commandManager.register("bingo") {
            literal("preview_card") {
                requires {
                    hasPermission(Permission.CONFIGURE_GAME) && hasState(GameState.PREGAME)
                }
                executes {
                    val options = scope.get<BingoOptions>()
                    options.showPreviewCard = !options.showPreviewCard

                    val events = scope.get<ScopedEvents>()
                    events.onChangeOptions.invoke(Unit)

                    Text.literal("ℹ  ")
                        .append(
                            text.string(StringKey.OptionsPreviewCard)
                                .append(": ${options.showPreviewCard}")
                                .formatted(Formatting.ITALIC)
                        )
                        .formatted(Formatting.AQUA)
                        .let { sendMessage(it) }
                }
            }
        }
    }
}