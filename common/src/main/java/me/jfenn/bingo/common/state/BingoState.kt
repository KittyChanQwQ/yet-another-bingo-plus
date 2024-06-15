package me.jfenn.bingo.common.state

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import me.jfenn.bingo.api.IMapService
import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.BingoCardEntry
import me.jfenn.bingo.common.config.BingoGoal
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.BlockPosition
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.game.GameEndReason
import me.jfenn.bingo.common.map.BingoMap
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.UuidAsString
import me.jfenn.bingo.common.utils.formatHHMMSS
import me.jfenn.bingo.common.utils.formatString
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.*
import kotlin.random.Random
import kotlin.time.Duration

@Serializable
data class BingoState(
    var isLobbyMode: Boolean = false,
    val gameId: UuidAsString = UUID.randomUUID(),
    var seed: Long = Random.nextLong(),
    var state: GameState = GameState.PREGAME,
    var startedAt: Instant? = null,
    var updatedAt: Instant? = null,
    var endedAt: Instant? = null,
    var lobbySpawnPos: BlockPosition = BlockPosition(0, 0, 0),
    var timeOffline: Duration = Duration.ZERO,
    val options: BingoOptions = ConfigService().options,
    val teams: MutableMap<BingoTeamKey, BingoTeam> = mutableMapOf(),
    var previewMap: BingoMap? = null,
    // This is a temporary card that should be overwritten by `CardService.generate()`
    var card: BingoCard = BingoCard(
        entries = List(25) { BingoCardEntry.FreeSpace() }
    ),
    var endReason: GameEndReason? = null,
    // Stores a set of recently-joined player UUIDs for [PlayerController]
    val playersJoinedIds: MutableSet<UuidAsString> = mutableSetOf(),
    // Stores if the game was ended manually (with "/bingo end")
    var isForfeit: Boolean = false,
) {

    fun getRegisteredTeams(): List<BingoTeam> {
        return teams.values.filter { it.players.isNotEmpty() }
    }

    fun registerTeam(team: BingoTeam): BingoTeam {
        return teams.getOrPut(team.key) { team.copy() }
    }

    fun getPreviewMap(mapService: IMapService): BingoMap {
        return previewMap ?: run {
            val map = BingoMap(
                mapId = mapService.getNextMapId(),
            )
            previewMap = map
            return@run map
        }
    }

    fun changeState(events: ScopedEvents, newState: GameState) {
        val prevState = this.state
        this.state = newState
        events.onStateChange(prevState to newState)
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
        val isWinner = when (val goal = options.cardGoal) {
            is BingoGoal.Items -> score.items >= goal.items
            is BingoGoal.Lines -> score.lines >= goal.lines
        }

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
        val updated = updatedAt ?: return null
        val duration = updated - started
        // subtract any offline server time from the game duration
        return duration - timeOffline
    }

    fun formatTimeElapsed(): MutableText {
        val duration = ingameDuration() ?: return Text.literal("0:00")
        return Text.literal(duration.formatHHMMSS())
    }

    fun formatTimeRemaining(
        textProvider: TextProvider,
        normalColor: Formatting = Formatting.WHITE,
        alertColor: Formatting = Formatting.RED,
    ): MutableText {
        val timeLimit = options.timeLimit
            ?: return textProvider.string(StringKey.TimerNoTimeLimit)

        val duration = ingameDuration() ?: return Text.literal(timeLimit.formatString()).formatted(normalColor)
        val secondsRemaining = timeLimit - duration

        if (duration >= timeLimit || state == GameState.POSTGAME)
            return textProvider.string(StringKey.TimerGameOver)

        return Text.literal(secondsRemaining.formatString())
            .formatted(if (secondsRemaining.inWholeSeconds <= 30 && secondsRemaining.inWholeSeconds % 2 == 0L) { alertColor } else { normalColor })
    }

}