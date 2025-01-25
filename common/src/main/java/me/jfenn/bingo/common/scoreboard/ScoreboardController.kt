package me.jfenn.bingo.common.scoreboard

import me.jfenn.bingo.common.config.PlayerSettingsService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.options.BingoOptions
import me.jfenn.bingo.common.options.BingoWinCondition
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.scoring.ScoreService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.TeamScore
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.MessageService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatString
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.IPlayerManager
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import kotlin.time.Duration

/**
 * Displays scoreboard score/timer information for all players.
 */
internal class ScoreboardController(
    events: ScopedEvents,
    private val options: BingoOptions,
    private val state: BingoState,
    private val teamService: TeamService,
    private val scoreboardService: ScoreboardService,
    private val playerManager: IPlayerManager,
    private val playerSettingsService: PlayerSettingsService,
    private val messageService: MessageService,
    private val text: TextProvider,
) : BingoComponent() {

    init {
        events.onEnter(GameState.PREGAME) {
            // reset the scoreboard views when entering a new game
            scoreboardService.clearScoreboards()
        }

        events.onUpdateTick {
            val isMultipleTeams = state.state == GameState.PREGAME || state.getRegisteredTeams().size > 1
            val isMultiCardWinCondition = when (
                val winCondition = options.winCondition
            ) {
                is BingoWinCondition.Cards -> winCondition.cards > 1
                is BingoWinCondition.Infinite -> true
                is BingoWinCondition.ReplaceGoals -> false
            }

            for (team in state.teams.values + null) {
                val objective = scoreboardService.getScoreboardObjective(team)
                val teamCard = team?.let { state.getCard(it) } ?: state.getActiveCard()

                val placeholders: Map<String, List<Text>> = buildMap {
                    options.formatGameModeIcons(teamCard)
                        .joinToString(" ")
                        .let {
                            when {
                                it.isEmpty() -> text.string(StringKey.OptionsModeStandard)
                                else -> Text.literal(it)
                            }
                        }
                        .let { text.string(StringKey.ScoreboardMode, it.formatted(Formatting.YELLOW)) }
                        .also { put("%mode%", listOf(it)) }

                    options.formatFeaturesIcons()
                        .joinToString(" ")
                        .let {
                            when {
                                it.isEmpty() -> text.string(StringKey.OptionsFeaturesNone)
                                else -> Text.literal(it)
                            }
                        }
                        .let { text.string(StringKey.ScoreboardFeatures, it.formatted(Formatting.YELLOW)) }
                        .also { put("%features%", listOf(it)) }

                    text.string(StringKey.ScoreboardGoal, state.formatCardGoals(team, text).formatted(Formatting.YELLOW))
                        .also { put("%goal%", listOf(it)) }

                    if (options.showRemainingTime && options.timeLimit != null) {
                        text.string(StringKey.ScoreboardTimeLeft, state.formatTimeRemaining(text, normalColor = Formatting.YELLOW))
                            .also { put("%time%", listOf(it)) }
                    } else if (options.timeLimit == null) {
                        val duration = (state.ingameDuration() ?: Duration.ZERO).formatString()
                        text.string(StringKey.ScoreboardTime, Text.literal(duration).formatted(Formatting.YELLOW))
                            .also { put("%time%", listOf(it)) }
                    } else {
                        put("%time%", emptyList())
                    }

                    team?.takeIf { isMultipleTeams }
                        ?.getName(text, playerName = true, symbol = true, bracketed = false, teamNameKey = null)
                        ?.let { text.string(StringKey.ScoreboardTeam, it) }
                        .also { put("%team%", listOfNotNull(it)) }

                    team?.score
                        ?.let { score ->
                            listOfNotNull(
                                text.string(StringKey.ScoreboardItemsScored, Text.literal("${score.items}")
                                    .formatted(team.textColor)),
                                text.string(StringKey.ScoreboardLinesScored, Text.literal("${score.lines}")
                                    .formatted(team.textColor)),
                                text.string(StringKey.ScoreboardCardsScored, Text.literal("${score.cards}")
                                    .formatted(team.textColor))
                                    .takeIf { isMultiCardWinCondition },
                            )
                        }
                        .also { put("%team_score%", it.orEmpty()) }

                    val leadingTeam = ScoreService.getLeading(state)
                    val leadingTeamName = leadingTeam?.getName(text, playerName = true, symbol = true, bracketed = false, teamNameKey = null)
                        ?: text.string(StringKey.ScoreboardLeadingTeamTie).formatted(Formatting.WHITE)
                    leadingTeamName?.takeIf { options.showLeadingTeam && isMultipleTeams }
                        ?.let { text.string(StringKey.ScoreboardLeadingTeam, it) }
                        .also { put("%leading%", listOfNotNull(it)) }

                    val leadingTeamFormatting = when {
                        // if the leading team should be hidden, don't use its formatting
                        !options.showLeadingTeam -> Formatting.WHITE
                        // otherwise, default to WHITE if the team is tied
                        else -> leadingTeam?.textColor ?: Formatting.WHITE
                    }

                    val leadingTeamScore = when {
                        leadingTeam == null -> TeamScore(
                            // if the leading team is a tie, find one of the tied teams to get the item/line/card count
                            items = ScoreService.getLeadingByItems(state, allowTies = true)?.score?.items ?: 0,
                            lines = ScoreService.getLeadingByLines(state, allowTies = true)?.score?.lines ?: 0,
                            cards = ScoreService.getLeadingByCards(state, allowTies = true)?.score?.cards ?: 0,
                        )
                        else -> leadingTeam.score
                    }

                    val showLeadingScore = (options.showCompletedItems || options.showCompletedLines) && isMultipleTeams && team != leadingTeam
                    val showLeadingScoreCards = isMultiCardWinCondition && leadingTeamScore.cards != team?.score?.cards

                    leadingTeamScore
                        .takeIf { showLeadingScore }
                        ?.let { score ->
                            listOfNotNull(
                                text.string(StringKey.ScoreboardItemsScored, Text.literal("${score.items}")
                                    .formatted(leadingTeamFormatting))
                                    .takeUnless { showLeadingScoreCards },
                                text.string(StringKey.ScoreboardLinesScored, Text.literal("${score.lines}")
                                    .formatted(leadingTeamFormatting))
                                    .takeUnless { showLeadingScoreCards },
                                text.string(StringKey.ScoreboardCardsScored, Text.literal("${score.cards}")
                                    .formatted(leadingTeamFormatting))
                                    .takeIf { showLeadingScoreCards },
                            )
                        }
                        .also { put("%leading_score%", it.orEmpty()) }
                }

                val scoreboardText = messageService.getLines(MessageService.MessageType.SCOREBOARD, placeholders)
                    .let { lines ->
                        val reducedLines: List<Text> = buildList {
                            for (line in lines) {
                                if (line.string.isEmpty() && lastOrNull()?.string?.isEmpty() == true)
                                    continue

                                add(line)
                            }
                        }
                        val emptyLines = reducedLines.reversed().indexOfFirst { it.string.isNotEmpty() }
                        reducedLines.subList(0, reducedLines.size - emptyLines)
                    }
                    .mapIndexed { index, text ->
                        if (text.string.isEmpty()) Text.literal(" ".repeat(index)) else text
                    }

                // Determine which players should be shown the scoreboard
                val players = playerManager.getPlayers()
                    .filter { teamService.getPlayerTeam(it.player)?.key == team?.key }
                    .filter { playerSettingsService.getPlayer(it).scoreboard }
                    .filter { scoreboardService.shouldDisplayScoreboard(it) }

                scoreboardService.setScoreboardContents(objective, scoreboardText, players.map { it.player })
            }
        }
    }

}