package me.jfenn.bingo.common.commands

import me.jfenn.bingo.api.commands.ICommandManager
import me.jfenn.bingo.api.commands.IExecutionContext
import me.jfenn.bingo.common.URL_WIKI
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.card.tierlist.TierListService
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.map.CardViewService
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
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
    private fun IExecutionContext.getOptionsText(): List<Text> = buildList {
        val state = scope.get<BingoState>()
        val options = scope.get<BingoOptions>()
        val tierListService = scope.get<TierListService>()
        val cardViewService = scope.get<CardViewService>()
        val card = cardViewService.getPlayerCard(player!!)

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
            options.formatGameMode(card)
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
            .append(state.formatCardGoals(card, text).formatted(Formatting.YELLOW))
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
                        Text.literal(card.options.itemDistribution[index].toString())
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
            .append(tierListService.formatTierLists(card.options).formatted(Formatting.YELLOW))
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
                getOptionsText().forEach { sendMessage(it) }
            }
        }
    }
}