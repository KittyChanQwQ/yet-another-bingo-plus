package me.jfenn.bingo.state

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import me.jfenn.bingo.BingoGame
import me.jfenn.bingo.card.BingoCard
import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.card.CardService
import me.jfenn.bingo.card.TeamScore
import me.jfenn.bingo.commands.Coords
import me.jfenn.bingo.config.BingoConfig
import me.jfenn.bingo.config.BlockPosition
import me.jfenn.bingo.config.ConfigService
import me.jfenn.bingo.utils.formatString
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
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
    var teamSpawnpoints: Map<BingoTeam, BlockPosition> = mapOf(),
    val teamChatCoordinates: MutableMap<BingoTeam, MutableList<Coords.CoordinatesEntry>> = mutableMapOf(),
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

        if (duration > timeLimit || state == GameState.POSTGAME)
            return Text.literal("(Game Over)").formatted(Formatting.BOLD)

        return Text.literal(secondsRemaining.formatString())
            .formatted(Formatting.BOLD, if (secondsRemaining.inWholeSeconds <= 30 && secondsRemaining.inWholeSeconds % 2 == 0L) { Formatting.RED } else { Formatting.WHITE })
    }

}