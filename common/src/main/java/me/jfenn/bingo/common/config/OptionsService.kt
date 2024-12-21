package me.jfenn.bingo.common.config

import me.jfenn.bingo.common.card.filter.ObjectiveFilterService
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.commands.BingoOptionsCommands
import me.jfenn.bingo.common.map.CardViewService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.joinText
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.IPlayerHandle
import net.minecraft.text.ClickEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.*

internal class OptionsService(
    private val state: BingoState,
    private val options: BingoOptions,
    private val cardViewService: CardViewService,
    private val objectiveFilterService: ObjectiveFilterService,
    private val teamService: TeamService,
    private val text: TextProvider,
) {
    fun getOptionsSummary(player: IPlayerHandle?): Sequence<MutableText> = sequence {
        val team = player?.let { teamService.getPlayerTeam(it) }
        val card = cardViewService.getPlayerCard(player)

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
            .let { yield(it) }

        text.string(
            StringKey.ScoreboardFeatures,
            options.formatFeatures()
                .map { text.string(it) }
                .joinText()
                .formatted(Formatting.YELLOW)
        )
            .formatted(Formatting.GRAY)
            .let { yield(it) }

        text.string(StringKey.OptionsGoal)
            .append(": ")
            .append(state.formatCardGoals(team, text).formatted(Formatting.YELLOW))
            .formatted(Formatting.GRAY)
            .also {
                it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bingo goal"))
            }
            .let { yield(it) }

        if (options.endGameWhen !is EndWhen.FirstWin) {
            text.string(
                StringKey.OptionsEndWhen,
                options.endGameWhen.string(text).formatted(Formatting.YELLOW)
            )
                .formatted(Formatting.GRAY)
                .also {
                    it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, BingoOptionsCommands.END_WHEN))
                }
                .let { yield(it) }
        }

        options.timeLimit?.let { timeLimit ->
            text.string(StringKey.OptionsTimeLimit)
                .append(": ")
                .append(
                    timeLimit.inWholeMinutes.let { minutes ->
                        String.format(Locale.US, "%dh %02dm", minutes / 60, minutes % 60)
                            .let { Text.literal(it).formatted(Formatting.YELLOW) }
                    }
                )
                .formatted(Formatting.GRAY)
                .also {
                    it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bingo timelimit"))
                }
                .let { yield(it) }
        }

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
                it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bingo difficulty"))
            }
            .let { yield(it) }

        text.string(StringKey.OptionsFilter)
            .append(": ")
            .append(objectiveFilterService.formatFilter(card.options.itemFilter).formatted(Formatting.YELLOW))
            .formatted(Formatting.GRAY)
            .also {
                it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bingo filter"))
            }
            .let { yield(it) }
    }
}