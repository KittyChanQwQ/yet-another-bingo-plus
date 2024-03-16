package me.jfenn.bingo.common.controller

import me.jfenn.bingo.common.card.ScoreService
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.PlayerSettingsService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.scoreboard.ScoreboardService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Formatting

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

    private fun String.wrap(maxChars: Int) : List<String> {
        val items = this.split(", ")
        return buildList {
            val builder = StringBuilder()
            for ((i, item) in items.withIndex()) {
                if (builder.isNotEmpty() && builder.length + item.length + 2 >= maxChars) {
                    add(builder.toString())
                    builder.clear()
                }

                builder.append(item)
                if (i+1 < items.size)
                    builder.append(", ")
            }

            if (builder.isNotEmpty())
                add(builder.toString())
        }
    }

    init {
        events.onUpdateTick {
            val activeTeams = state.getRegisteredTeams()
            val isMultipleTeams = state.state == GameState.PREGAME || activeTeams.size > 1

            for (team in activeTeams + null) {
                val objective = scoreboardService.getScoreboardObjective(team)

                val scoreboardText = buildList {
                    add("")
                    textProvider.raw(StringKey.ScoreboardMode, "${Formatting.YELLOW}" + options.formatGameMode())
                        .wrap(32)
                        .mapIndexed { i, str ->
                            if (i == 0) str else "  ${Formatting.YELLOW}$str"
                        }
                        .also { addAll(it) }
                    textProvider.raw(StringKey.ScoreboardFeatures, "${Formatting.YELLOW}" + options.formatFeatures())
                        .wrap(32)
                        .mapIndexed { i, str ->
                            if (i == 0) str else "  ${Formatting.YELLOW}$str"
                        }
                        .also { addAll(it) }
                    textProvider.raw(StringKey.ScoreboardGoal, "${Formatting.YELLOW}" + textProvider.raw(options.goal.string))
                        .also { add(it) }
                    if (options.showRemainingTime && options.timeLimit != null) {
                        textProvider.raw(StringKey.ScoreboardTimeLeft, "${Formatting.YELLOW}${state.formatTimeRemaining().string}")
                            .also { add(it) }
                    }
                    add("")
                    if (team != null) {
                        val score = team.score

                        if (isMultipleTeams) {
                            textProvider.raw(StringKey.ScoreboardTeam, "${team.formatting}${team.name}")
                                .also { add(it) }
                        }
                        textProvider.raw(StringKey.ScoreboardItemsScored, "${team.formatting}${score.items}")
                            .also { add(it) }
                        textProvider.raw(StringKey.ScoreboardLinesScored, "${team.formatting}${score.lines}")
                            .also { add(it) }
                        add("")
                    }

                    val leadingTeam = ScoreService.getLeading(state)
                    if (options.showLeadingTeam && isMultipleTeams) {
                        val teamName = leadingTeam?.let { it.formatting.toString() + it.name }
                            ?: "${Formatting.WHITE}${textProvider.raw(StringKey.ScoreboardLeadingTeamTie)}"
                        textProvider.raw(StringKey.ScoreboardLeadingTeam, teamName)
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
                        textProvider.raw(StringKey.ScoreboardItemsScored, "$formatting$itemsScored")
                            .also { add(it) }
                    }
                    if (options.showCompletedLines && isMultipleTeams && team != leadingTeam) {
                        // if the leading team is a tie, find one of the tied teams to get the item/line count
                        val leadingOrTied = leadingTeam ?: ScoreService.getLeadingByLines(state, allowTies = true)
                        val linesScored = leadingOrTied?.score?.lines ?: 0
                        textProvider.raw(StringKey.ScoreboardLinesScored, "$formatting$linesScored")
                            .also { add(it) }
                    }

                    if (lastOrNull() != "")
                        add("")
                }.mapIndexed { i, str ->
                    str.ifEmpty { " ".repeat(i) }
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