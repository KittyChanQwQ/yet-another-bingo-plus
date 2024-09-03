package me.jfenn.bingo.common.state

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import me.jfenn.bingo.api.IMapService
import me.jfenn.bingo.common.card.BingoCard
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
import kotlin.time.Duration

@Serializable
data class BingoState(
    var isLobbyMode: Boolean = false,
    var gameId: UuidAsString = UUID.randomUUID(),
    var state: GameState = GameState.UNINITIALIZED,
    var startedAt: Instant? = null,
    var updatedAt: Instant? = null,
    var endedAt: Instant? = null,
    var lobbySpawnPos: BlockPosition = BlockPosition(0, 0, 0),
    var lobbySpawnYaw: Float = 0f,
    var timeOffline: Duration = Duration.ZERO,
    val options: BingoOptions = ConfigService().options,
    val teams: MutableMap<BingoTeamKey, BingoTeam> = mutableMapOf(),
    var previewMap: BingoMap? = null,
    // This is a temporary list that should be overwritten by `CardService.generate()`
    val cards: MutableList<BingoCard> = mutableListOf(),
    var endReason: GameEndReason? = null,
    // Stores a set of recently-joined player UUIDs for [PlayerController]
    val playersJoinedIds: MutableSet<UuidAsString> = mutableSetOf(),
    // Stores if the game was ended manually (with "/bingo end")
    var isForfeit: Boolean = false,
) {

    fun getActiveCard(): BingoCard {
        return cards.firstOrNull()
            ?: throw IllegalArgumentException("No cards in state")
    }

    fun pushCard(card: BingoCard) {
        card.nextCardId = cards.firstOrNull()?.id
        cards.add(0, card)
        options.cards = cards.map { it.options }
    }

    fun pushCardTail(card: BingoCard) {
        cards.lastOrNull()?.nextCardId = card.id
        card.nextCardId = null
        cards.add(card)
        options.cards = cards.map { it.options }
    }

    fun popCard() {
        cards.removeFirst()
        options.cards = cards.map { it.options }
    }

    fun replaceCard(card: BingoCard) {
        val index = cards.indexOfFirst { it.id == card.id }
            .takeIf { it != -1 }
            ?: throw IllegalArgumentException("Unknown card provided in replaceCard")

        cards[index] = card
        options.cards = cards.map { it.options }
    }

    fun getCard(team: BingoTeam): BingoCard? {
        return team.card?.let { cardId -> cards.find { it.id == cardId } }
    }

    fun getOrAssignCard(team: BingoTeam): BingoCard {
        val card = team.card?.let { cardId -> cards.find { it.id == cardId } }
        return if (card == null) {
            val firstCard = cards.first()
            team.card = firstCard.id
            firstCard
        } else {
            card
        }
    }

    /**
     * Returns true if there is only one player in the game
     */
    fun isSingleplayer() = teams.values.flatMap { it.players }
        .distinctBy { it.uuid }
        .count() == 1

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

    fun formatCardGoals(card: BingoCard, text: TextProvider): MutableText {
        val index = cards.indexOf(card)
        val remainingCards = cards.size - 1 - index
        return card.options.goal.format(text)
            .also {
                if (remainingCards > 0)
                    it.append(Text.literal(" +$remainingCards").formatted(Formatting.BOLD))
            }
    }

    /**
     * If a game is over & we want to reset this to its default state
     * without restarting...
     * (should only be called when isLobbyMode=false)
     */
    fun reset() {
        // set a new gameId
        gameId = UUID.randomUUID()
        startedAt = null
        updatedAt = null
        endedAt = null
        timeOffline = Duration.ZERO
        teams.clear()
        // previewMap we don't clear, as it can be reused...
        // [BingoMapController] calls generateCard when entering PREGAME, so this card should be immediately replaced
        cards.clear()
        cards.add(BingoCard.EMPTY)
        endReason = null
        isForfeit = false
    }

}