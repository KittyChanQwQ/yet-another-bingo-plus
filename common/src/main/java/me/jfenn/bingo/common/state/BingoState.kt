package me.jfenn.bingo.common.state

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.BingoTeamKey
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.config.BlockPosition
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.map.BingoMap
import me.jfenn.bingo.common.utils.formatString
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import kotlin.random.Random
import kotlin.time.Duration

@Serializable
data class BingoState(
    var seed: Long = Random.nextLong(),
    var state: GameState = GameState.PREGAME,
    var startedAt: Instant? = null,
    var updatedAt: Instant? = null,
    var lobbySpawnPos: BlockPosition = BlockPosition(0, 0, 0),
    var timeOffline: Duration = Duration.ZERO,
    var config: BingoConfig = ConfigService.config,
    val teams: MutableMap<BingoTeamKey, BingoTeam> = mutableMapOf(),
    var previewMap: BingoMap? = null,
    var card: BingoCard = CardService.generate(config, seed),
) : SerializedPersistentState() {

    fun getRegisteredTeams(): List<BingoTeam> {
        return teams.values.filter { it.players.isNotEmpty() }
    }

    fun registerTeam(team: BingoTeam): BingoTeam {
        return teams.getOrPut(team.key) { team }
    }

    fun getPreviewMap(ctx: BingoContext): BingoMap {
        return previewMap ?: run {
            val map = BingoMap(
                mapId = ctx.server.overworld.nextMapId,
            )
            previewMap = map
            return@run map
        }
    }

    fun changeState(ctx: BingoContext, newState: GameState) {
        val prevState = this.state
        this.state = newState
        newState.onEnter(ctx, prevState)
    }

    fun updateScore(team: BingoTeam): TeamScore {
        val score = card.getTeamScore(team)
        team.score = score
        return score
    }

    /**
     * @return true if the team is now a winner (and was not before)
     */
    fun updateWinner(team: BingoTeam): Boolean {
        val score = team.score
        val isWinner = score.lines >= config.winCondition.numLines

        if (isWinner && team.winner == null) {
            team.winner = TeamWinner(
                instant = Clock.System.now()
            )
            return true
        }

        return false
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

        if (duration >= timeLimit || state == GameState.POSTGAME)
            return Text.literal("(Game Over)").formatted(Formatting.BOLD)

        return Text.literal(secondsRemaining.formatString())
            .formatted(Formatting.BOLD, if (secondsRemaining.inWholeSeconds <= 30 && secondsRemaining.inWholeSeconds % 2 == 0L) { Formatting.RED } else { Formatting.WHITE })
    }

    fun formatWinners(): List<Text> = buildList {
        // List all teams that have completed the card
        getRegisteredTeams()
            .filter { it.isWinner() }
            .mapNotNull { team ->
                team.winner?.let { team to it }
            }
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
        getRegisteredTeams()
            .filter { !it.isWinner() }
            .forEach { team ->
                Text.empty()
                    .append(Text.literal("DNF - ").formatted(Formatting.GRAY))
                    .append(team.text())
                    .also { add(it) }
            }
    }

}