package me.jfenn.bingo.common.integrations

import me.jfenn.bingo.api.IObjectiveHandle
import me.jfenn.bingo.api.IScoreboardManager
import me.jfenn.bingo.common.Bingo
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.ScoreService
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.map.MapItemService
import me.jfenn.bingo.common.state.BingoContext
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.world.GameMode
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Displays scoreboard score/timer information for all players.
 */
object ScoreboardController : KoinComponent {

    val scoreboardManager by inject<IScoreboardManager>()

    fun getScoreboardObjective(ctx: BingoContext, team: BingoTeam?) : IObjectiveHandle {
        val objectiveName = "bingo_sidebar_" + team?.name
        val objective = scoreboardManager.createDummyObjective(ctx.server, objectiveName, Text.literal("Minecraft BINGO"))
        return objective
    }

    fun shouldDisplayScoreboard(ctx: BingoContext, playerEntity: PlayerEntity) : Boolean {
        val isScoreboardEnabled = ConfigService.getPlayer(playerEntity.uuidAsString).scoreboard
        val player = ctx.server.playerManager.getPlayer(playerEntity.uuid) ?: return false

        // if the player is holding the map item, show the scoreboard; otherwise, hide it
        val isHoldingMapItem = player.interactionManager.gameMode != GameMode.SURVIVAL
                || MapItemService.isMapItem(playerEntity.mainHandStack)
                || MapItemService.isMapItem(playerEntity.offHandStack)

        return isScoreboardEnabled && isHoldingMapItem
    }

    fun init() {
        ConfigService.onChangePlayerSettings { player ->
            val settings = ConfigService.getPlayer(player.uuidAsString)

            if (!settings.scoreboard) {
                // send a packet remove the scoreboard sidebar if set to false
                scoreboardManager.sendScoreboardDisplay(player, null)
            }
        }

        Bingo.onUpdateTick {
            for (team in BingoTeam.entries + null) {
                val objective = getScoreboardObjective(this, team)

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
                        val score = state.getTeamScore(team)

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
                        val itemsScored = leadingOrTied?.let { state.getTeamScore(it).items } ?: 0
                        add("Items scored: $formatting$itemsScored")
                    }
                    if (config.showCompletedLines && team != leadingTeam) {
                        // if the leading team is a tie, find one of the tied teams to get the item/line count
                        val leadingOrTied = leadingTeam ?: ScoreService.getLeadingByLines(state, allowTies = true)
                        val linesScored = leadingOrTied?.let { state.getTeamScore(it).lines } ?: 0
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
                    .filter { BingoTeam.fromPlayer(it) == team }
                    .filter { ConfigService.getPlayer(it.uuidAsString).scoreboard }
                    .forEach { player ->
                        // if the player is holding the map item, show the scoreboard; otherwise, hide it
                        if (shouldDisplayScoreboard(this, player)) {
                            scoreboardManager.sendScoreboardUpdates(player, objective)
                            scoreboardManager.sendScoreboardDisplay(player, objective)
                        } else {
                            scoreboardManager.sendScoreboardDisplay(player, null)
                        }
                    }
            }
        }
    }

}