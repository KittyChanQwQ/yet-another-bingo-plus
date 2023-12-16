package me.jfenn.bingo.common.state

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.commands.CoordsCommand
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.config.BlockPosition
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.utils.formatString
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import kotlin.time.Duration

@Serializable
data class BingoState(
    var state: GameState = GameState.PREGAME,
    var startedAt: Instant? = null,
    var updatedAt: Instant? = null,
    var lobbySpawnPos: BlockPosition = BlockPosition(0, 0, 0),
    var timeOffline: Duration = Duration.ZERO,
    var config: BingoConfig = ConfigService.config,
    val teamScores: MutableMap<BingoTeam, TeamScore> = mutableMapOf(),
    val teamWinners: MutableMap<BingoTeam, TeamWinner> = mutableMapOf(),
    var teamSpawnpoints: MutableMap<BingoTeam, BlockPosition> = mutableMapOf(),
    val teamChatCoordinates: MutableMap<BingoTeam, MutableList<CoordsCommand.CoordinatesEntry>> = mutableMapOf(),
    val teamMapIds: MutableMap<BingoTeam, Int> = mutableMapOf(),
    var card: BingoCard = CardService.generate(config),
) : SerializedPersistentState() {

    fun changeState(ctx: BingoContext, newState: GameState) {
        val prevState = this.state
        this.state = newState
        newState.onEnter(ctx, prevState)
    }

    fun updateScore(team: BingoTeam): TeamScore {
        val score = card.getTeamScore(team)
        teamScores[team] = score
        return score
    }

    fun getTeamScore(team: BingoTeam): TeamScore {
        return teamScores[team] ?: TeamScore.ZERO
    }

    /**
     * @return true if the team is now a winner (and was not before)
     */
    fun updateWinner(team: BingoTeam): Boolean {
        val score = getTeamScore(team)
        val isWinner = score.lines >= config.winCondition.numLines

        if (isWinner && !teamWinners.containsKey(team)) {
            teamWinners[team] = TeamWinner(
                instant = Clock.System.now()
            )
            return true
        }

        return false
    }

    fun getWinner(team: BingoTeam): TeamWinner? {
        return teamWinners[team]
    }

    fun isWinner(team: BingoTeam) = getWinner(team) != null

    fun ingameDuration(): Duration? {
        val started = startedAt ?: return null
        val duration = Clock.System.now() - started
        // subtract any offline server time from the game duration
        return duration - timeOffline
    }

    fun formatTimeRemaining(): Text {
        val timeLimit = config.timeLimit
            ?: return Text.literal("(No Time Limit)").formatted(Formatting.BOLD)

        val duration = ingameDuration() ?: return Text.literal(timeLimit.formatString())
        val secondsRemaining = timeLimit - duration

        if (duration >= timeLimit || state == GameState.POSTGAME)
            return Text.literal("(Game Over)").formatted(Formatting.BOLD)

        return Text.literal(secondsRemaining.formatString())
            .formatted(Formatting.BOLD, if (secondsRemaining.inWholeSeconds <= 30 && secondsRemaining.inWholeSeconds % 2 == 0L) { Formatting.RED } else { Formatting.WHITE })
    }

    fun formatWinners(): List<Text> = buildList {
        // List all teams that have completed the card
        teamWinners.entries
            .sortedBy { (_, win) -> win.instant }
            .forEachIndexed { index, (team, win) ->
                val duration = win.instant - (startedAt ?: Clock.System.now())
                Text.empty()
                    .append(Text.literal("${index+1}. ").formatted(Formatting.GRAY))
                    .append(team.text())
                    .append(" - ${duration.formatString()}")
                    .also { add(it) }
            }

        // List any teams that started the card but did not finish
        (teamScores.keys - teamWinners.keys)
            .forEach { team ->
                Text.empty()
                    .append(Text.literal("DNF - ").formatted(Formatting.GRAY))
                    .append(team.text())
                    .also { add(it) }
            }
    }

}