package me.jfenn.bingo.common.scoreboard

import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.PlayerSettingsService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.scoring.ScoreService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatString
import me.jfenn.bingo.common.utils.joinText
import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.MinecraftServer
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import kotlin.time.Duration

/**
 * Displays scoreboard score/timer information for all players.
 */
class ScoreboardController(
    events: ScopedEvents,
    private val server: MinecraftServer,
    private val options: BingoOptions,
    private val state: BingoState,
    private val teamService: TeamService,
    private val scoreboardService: ScoreboardService,
    private val playerSettingsService: PlayerSettingsService,
    private val textProvider: TextProvider,
) : BingoComponent() {

    private fun List<Text>.wrapText(maxItems: Int) : List<MutableText> {
        val ret = mutableListOf<MutableText>()
        val page = mutableListOf<Text>()
        for (text in this) {
            page.add(text)
            if (page.size == maxItems) {
                ret.add(page.joinText())
                page.clear()
            }
        }

        if (page.isNotEmpty()) {
            ret.add(page.joinText())
        }

        return ret
    }

    init {
        events.onEnter(GameState.PREGAME) {
            // reset the scoreboard views when entering a new game
            scoreboardService.clearScoreboards()
        }

        events.onUpdateTick {
            val activeTeams = state.getRegisteredTeams()
            val isMultipleTeams = state.state == GameState.PREGAME || activeTeams.size > 1

            for (team in activeTeams + null) {
                val objective = scoreboardService.getScoreboardObjective(team)

                val scoreboardText = buildList<Text?> {
                    add(null)

                    options.formatGameMode()
                        .map { textProvider.string(it) }
                        .wrapText(2)
                        .forEachIndexed { index, t ->
                            add(when {
                                index == 0 -> textProvider.string(StringKey.ScoreboardMode, t.formatted(Formatting.YELLOW))
                                else -> Text.literal("  ").append(t).formatted(Formatting.YELLOW)
                            })
                        }

                    options.formatFeatures()
                        .map { textProvider.string(it) }
                        .wrapText(2)
                        .forEachIndexed { index, t ->
                            add(when {
                                index == 0 -> textProvider.string(StringKey.ScoreboardFeatures, t.formatted(Formatting.YELLOW))
                                else -> Text.literal("  ").append(t).formatted(Formatting.YELLOW)
                            })
                        }

                    textProvider.string(StringKey.ScoreboardGoal, Text.empty().append(options.cardGoal.format(textProvider)).formatted(Formatting.YELLOW))
                        .also { add(it) }
                    if (options.showRemainingTime && options.timeLimit != null) {
                        textProvider.string(StringKey.ScoreboardTimeLeft, state.formatTimeRemaining(textProvider, normalColor = Formatting.YELLOW))
                            .also { add(it) }
                    } else if (options.timeLimit == null) {
                        val duration = (state.ingameDuration() ?: Duration.ZERO).formatString()
                        textProvider.string(StringKey.ScoreboardTime, Text.literal(duration).formatted(Formatting.YELLOW))
                            .also { add(it) }
                    }
                    add(null)
                    if (team != null) {
                        val score = team.score

                        if (isMultipleTeams) {
                            val teamName = Text.literal("${team.symbol} ")
                                .append(team.singlePlayer?.name?.let { Text.literal(it) } ?: textProvider.string(team.name))
                                .formatted(team.formatting)
                            textProvider.string(StringKey.ScoreboardTeam, teamName)
                                .also { add(it) }
                        }
                        textProvider.string(StringKey.ScoreboardItemsScored, Text.literal("${score.items}").formatted(team.formatting))
                            .also { add(it) }
                        textProvider.string(StringKey.ScoreboardLinesScored, Text.literal("${score.lines}").formatted(team.formatting))
                            .also { add(it) }
                        add(null)
                    }

                    val leadingTeam = ScoreService.getLeading(state)
                    if (options.showLeadingTeam && isMultipleTeams) {
                        val teamName = leadingTeam?.let {
                            Text.literal("${it.symbol} ")
                                .append(it.singlePlayer?.name?.let { Text.literal(it) } ?: textProvider.string(it.name))
                                .formatted(it.formatting)
                        }
                            ?: textProvider.string(StringKey.ScoreboardLeadingTeamTie).formatted(Formatting.WHITE)

                        textProvider.string(StringKey.ScoreboardLeadingTeam, teamName)
                            .also { add(it) }
                    }

                    val formatting = when {
                        // if the leading team should be hidden, don't use its formatting
                        !options.showLeadingTeam -> Formatting.WHITE
                        // otherwise, default to WHITE if the team is tied
                        else -> leadingTeam?.formatting ?: Formatting.WHITE
                    }

                    if (options.showCompletedItems && isMultipleTeams && team != leadingTeam) {
                        // if the leading team is a tie, find one of the tied teams to get the item/line count
                        val leadingOrTied = leadingTeam ?: ScoreService.getLeadingByItems(state, allowTies = true)
                        val itemsScored = leadingOrTied?.score?.items ?: 0
                        textProvider.string(StringKey.ScoreboardItemsScored, Text.literal("$itemsScored").formatted(formatting))
                            .also { add(it) }
                    }
                    if (options.showCompletedLines && isMultipleTeams && team != leadingTeam) {
                        // if the leading team is a tie, find one of the tied teams to get the item/line count
                        val leadingOrTied = leadingTeam ?: ScoreService.getLeadingByLines(state, allowTies = true)
                        val linesScored = leadingOrTied?.score?.lines ?: 0
                        textProvider.string(StringKey.ScoreboardLinesScored, Text.literal("$linesScored").formatted(formatting))
                            .also { add(it) }
                    }

                    if (lastOrNull() != null)
                        add(null)
                }.mapIndexed { i, text ->
                    text ?: Text.literal(" ".repeat(i))
                }

                // Determine which players should be shown the scoreboard
                val players = server.playerManager.playerList
                    .filter { teamService.getPlayerTeam(it)?.key == team?.key }
                    .filter { playerSettingsService.getPlayer(it).scoreboard }
                    .filter { scoreboardService.shouldDisplayScoreboard(it) }

                scoreboardService.setScoreboardContents(objective, scoreboardText, players)
            }
        }
    }

}