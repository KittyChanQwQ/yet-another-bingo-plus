package me.jfenn.bingo.common.commands

import me.jfenn.bingo.api.commands.ICommandManager
import me.jfenn.bingo.common.URL_WIKI
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.TierLabel
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatTitle
import me.jfenn.bingo.common.utils.joinText
import me.jfenn.bingo.generated.BINGO_VERSION
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.ClickEvent
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.*

class BingoCommand(
    commandManager: ICommandManager,
    private val text: TextProvider,
) : BingoComponent() {
    private fun getOptionsText(
        options: BingoOptions,
    ): List<Text> = buildList {
        add(Text.empty())

        text.string(StringKey.FullName)
            .append(" v$BINGO_VERSION")
            .formatted(Formatting.DARK_GREEN, Formatting.UNDERLINE).also {
                it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, URL_WIKI))
            }
            .let { add(it) }

        add(Text.empty())

        text.string(
            StringKey.ScoreboardMode,
            options.formatGameMode()
                .map { text.string(it) }
                .joinText()
                .formatted(Formatting.YELLOW)
        )
            .formatted(Formatting.GRAY)
            .also {
                it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bingo mode"))
            }
            .let { add(it) }

        text.string(
            StringKey.ScoreboardFeatures,
            options.formatFeatures()
                .map { text.string(it) }
                .joinText()
                .formatted(Formatting.YELLOW)
        )
            .formatted(Formatting.GRAY)
            .let { add(it) }

        text.string(StringKey.OptionsGoal)
            .append(": ")
            .append(options.cardGoal.format(text).formatted(Formatting.YELLOW))
            .formatted(Formatting.GRAY)
            .also {
                it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bingo goal"))
            }
            .let { add(it) }

        text.string(StringKey.OptionsTimeLimit)
            .append(": ")
            .append(
                options.timeLimit?.inWholeMinutes
                    ?.let { minutes ->
                        String.format(Locale.US, "%dh %02dm", minutes / 60, minutes % 60)
                            .let { Text.literal(it).formatted(Formatting.YELLOW) }
                    }
                    ?: text.string(StringKey.OptionsTimeLimitOff).formatted(Formatting.YELLOW)
            )
            .formatted(Formatting.GRAY)
            .also {
                it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bingo timelimit"))
            }
            .let { add(it) }

        text.string(StringKey.OptionsItemDistribution)
            .append(": ")
            .append(
                TierLabel.entries
                    .mapIndexed { index, label ->
                        Text.literal(options.itemDistribution[index].toString())
                            .formatted(label.formatting)
                    }
                    .joinText(Text.literal(" "))
            )
            .formatted(Formatting.GRAY)
            .also {
                it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bingo itemdist"))
            }
            .let { add(it) }

        text.string(StringKey.OptionsTierList)
            .append(": ")
            .append(Text.literal(options.tierList.formatTitle()).formatted(Formatting.YELLOW))
            .formatted(Formatting.GRAY)
            .also {
                it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bingo tierlist"))
            }
            .let { add(it) }

        add(Text.empty())
    }

    init {
        commandManager.register("bingo") {
            executes {
                val options = scope.get<BingoOptions>()
                getOptionsText(options).forEach { sendMessage(it) }
            }
        }
    }
}