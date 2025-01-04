package me.jfenn.bingo.common.commands

import me.jfenn.bingo.platform.commands.ICommandManager
import me.jfenn.bingo.platform.commands.IExecutionContext
import me.jfenn.bingo.common.URL_WIKI
import me.jfenn.bingo.common.options.OptionsService
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.BINGO_VERSION
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.ClickEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class BingoCommand(
    commandManager: ICommandManager,
    private val text: TextProvider,
) : BingoComponent() {
    private fun IExecutionContext.getOptionsText(): List<MutableText> = buildList {
        add(Text.empty())

        text.string(StringKey.FullName)
            .append(" v$BINGO_VERSION")
            .formatted(Formatting.DARK_GREEN, Formatting.UNDERLINE).also {
                it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, URL_WIKI))
            }
            .let { add(it) }

        add(Text.empty())

        addAll(scope.get<OptionsService>().getOptionsSummary(player))

        add(Text.empty())
    }

    init {
        commandManager.register("bingo") {
            executes {
                getOptionsText().forEach { sendMessage(it) }
            }
        }
    }
}