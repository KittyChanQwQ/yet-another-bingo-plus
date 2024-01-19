package me.jfenn.bingo.common.integrations

import me.jfenn.bingo.api.IObjectiveHandle
import me.jfenn.bingo.api.IScoreboardManager
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.ScoreService
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.map.MapItemService
import me.jfenn.bingo.common.state.BingoState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.world.GameMode
import java.util.*

/**
 * Displays scoreboard score/timer information for all players.
 */
class ScoreboardController(
    events: ScopedEvents,
    private val server: MinecraftServer,
    private val config: BingoConfig,
    private val state: BingoState,
    private val teamService: TeamService,
    private val scoreboardHandler: ScoreboardHandler,
    private val scoreboardManager: IScoreboardManager,
) {

    init {
        events.onChangePlayerSettings { player ->
            val settings = ConfigService.getPlayer(player.uuidAsString)

            if (!settings.scoreboard) {
                // send a packet remove the scoreboard sidebar if set to false
                scoreboardManager.sendScoreboardDisplay(player, null)
            }
        }

        events.onUpdateTick {
            for (team in state.getRegisteredTeams() + null) {
                val objective = scoreboardHandler.getScoreboardObjective(team)

                val scoreboardText = buildList {
                    add("")
                    config.formatGameMode().let {
                        if (it.length > 18) {
                            add("Game mode:")
                            add("  ${Formatting.YELLOW}$it")
                        } else {
                            add("Game mode: ${Formatting.YELLOW}$it")
                        }
                    }
                    config.formatFeatures().let {
                        if (it.length > 18) {
                            add("Features:")
                            add("  ${Formatting.YELLOW}$it")
                        } else {
                            add("Features: ${Formatting.YELLOW}$it")
                        }
                    }
                    add("Win condition: ${Formatting.YELLOW}${config.winCondition.formatName()}")
                    if (config.showRemainingTime) {
                        add("Time left: ${Formatting.YELLOW}${state.formatTimeRemaining().string}")
                    }
                    add("")
                    if (team != null) {
                        val score = team.score

                        add("Team: ${team.formatting}${team.name}")
                        add("Items scored: ${team.formatting}${score.items}")
                        add("Lines scored: ${team.formatting}${score.lines}")
                        add("")
                    }

                    val leadingTeam = ScoreService.getLeading(state)
                    if (config.showLeadingTeam) {
                        add("Leading team: ${leadingTeam?.let { it.formatting.toString() + it.name } ?: "${Formatting.WHITE}Tie"}")
                    }

                    val formatting = when {
                        // if the leading team should be hidden, don't use its formatting
                        !config.showLeadingTeam -> Formatting.WHITE
                        // otherwise, default to WHITE if the team is tied
                        else -> leadingTeam?.formatting ?: Formatting.WHITE
                    }

                    if (config.showCompletedItems && team != leadingTeam) {
                        // if the leading team is a tie, find one of the tied teams to get the item/line count
                        val leadingOrTied = leadingTeam ?: ScoreService.getLeadingByItems(state, allowTies = true)
                        val itemsScored = leadingOrTied?.score?.items ?: 0
                        add("Items scored: $formatting$itemsScored")
                    }
                    if (config.showCompletedLines && team != leadingTeam) {
                        // if the leading team is a tie, find one of the tied teams to get the item/line count
                        val leadingOrTied = leadingTeam ?: ScoreService.getLeadingByLines(state, allowTies = true)
                        val linesScored = leadingOrTied?.score?.lines ?: 0
                        add("Lines scored: $formatting$linesScored")
                    }

                    if (config.showLeadingTeam || config.showCompletedItems || config.showCompletedLines)
                        add("")
                }.mapIndexed { i, str ->
                    str.ifEmpty { " ".repeat(i) }
                }

                // Update the scoreboard text on the server
                scoreboardManager.setScoreboardText(server, objective, scoreboardText)

                // Manually send the scoreboard update packets to each player
                server.playerManager.playerList
                    .filter { teamService.getPlayerTeam(it)?.key == team?.key }
                    .filter { ConfigService.getPlayer(it.uuidAsString).scoreboard }
                    .forEach { player ->
                        // if the player is holding the map item, show the scoreboard; otherwise, hide it
                        if (scoreboardHandler.shouldDisplayScoreboard(player)) {
                            scoreboardManager.sendScoreboardUpdates(
                                player, objective,
                                sendCreate = !scoreboardHandler.playerHasObjective(player, objective),
                            )
                            scoreboardManager.sendScoreboardDisplay(player, objective)
                        } else {
                            scoreboardManager.sendScoreboardDisplay(player, null)
                        }
                    }
            }
        }

        events.onPlayerLeave { player ->
            // When a player disconnects, they need to be re-sent the objective if they reconnect
            scoreboardHandler.playersWithObjectives.remove(player.uuid)
        }
    }

}