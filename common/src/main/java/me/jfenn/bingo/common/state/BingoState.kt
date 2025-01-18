package me.jfenn.bingo.common.state

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.game.GameOverService
import me.jfenn.bingo.common.map.BingoMap
import me.jfenn.bingo.common.menu.MenuPage
import me.jfenn.bingo.common.menu.MenuState
import me.jfenn.bingo.common.options.BingoOptions
import me.jfenn.bingo.common.options.BingoWinCondition
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.BlockPositionType
import me.jfenn.bingo.common.utils.UuidAsString
import me.jfenn.bingo.common.utils.formatHHMMSS
import me.jfenn.bingo.common.utils.formatString
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.IMapService
import me.jfenn.bingo.platform.block.BlockPosition
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
    val menu: MenuState = MenuState(),
    var startedAt: Instant? = null,
    var updatedAt: Instant? = null,
    var endedAt: Instant? = null,
    var lobbySpawnPos: BlockPositionType = BlockPosition(0, 0, 0),
    var lobbySpawnYaw: Float = 0f,
    var timeOffline: Duration = Duration.ZERO,
    val options: BingoOptions = ConfigService().options,
    val teams: MutableMap<BingoTeamKey, BingoTeam> = mutableMapOf(),
    var previewMap: BingoMap? = null,
    // This is a temporary list that should be overwritten by `CardService.generate()`
    val cards: MutableList<BingoCard> = mutableListOf(),
    var gameOverInfo: GameOverService.GameOverInfo? = null,
    // Stores a set of recently-joined player UUIDs for [PlayerController]
    val playersJoinedIds: MutableSet<UuidAsString> = mutableSetOf(),
    // Stores if the game was ended manually (with "/bingo end")
    var isForfeit: Boolean = false,
) {

    fun getActiveCard(): BingoCard {
        return cards.firstOrNull()
            ?: throw IllegalArgumentException("No cards in state")
    }

    /**
     * Replace any assigned team cards with the default behavior (first card)
     */
    fun resetTeamCards() {
        for (team in teams.values) {
            team.cardId = cards.first().id
        }
    }

    fun pushCard(card: BingoCard) {
        val nextCard = cards.firstOrNull()
        card.nextCardId = nextCard?.id
        cards.add(0, card)
    }

    fun pushCardTail(card: BingoCard) {
        cards.lastOrNull()?.nextCardId = card.id
        card.nextCardId = null
        cards.add(card)
    }

    fun popCard() {
        if (cards.size == 1) return

        val removedCard = cards.removeFirst()

        val firstCard = cards.first()
        for (team in teams.values) {
            if (team.cardId == removedCard.id)
                team.cardId = firstCard.id
        }
    }

    fun replaceCard(card: BingoCard) {
        val index = cards.indexOfFirst { it.id == card.id }
            .takeIf { it != -1 }
            ?: throw IllegalArgumentException("Unknown card provided in replaceCard")

        cards[index] = card
    }

    fun getCard(team: BingoTeam): BingoCard? {
        return if (team.isWinner() || state == GameState.POSTGAME) {
            team.completedCards.lastOrNull()?.card
        } else {
            team.cardId?.let { cardId -> cards.find { it.id == cardId } }
        }
    }

    fun getCard(id: UUID): BingoCard? {
        return cards.find { it.id == id }
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
        return teams.getOrPut(team.key) {
            team.newInstance().apply {
                cardId = cards.first().id
            }
        }
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

    fun remainingDuration(): Duration? {
        val timeLimit = options.timeLimit ?: return null
        val duration = ingameDuration() ?: return null
        return timeLimit - duration
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

        if (startedAt == null) return Text.literal(timeLimit.formatString()).formatted(normalColor)
        val secondsRemaining = remainingDuration() ?: return Text.literal(timeLimit.formatString()).formatted(normalColor)

        if (secondsRemaining <= Duration.ZERO || state == GameState.POSTGAME)
            return textProvider.string(StringKey.TimerGameOver)

        return Text.literal(secondsRemaining.formatString())
            .formatted(if (secondsRemaining.inWholeSeconds <= 30 && secondsRemaining.inWholeSeconds % 2 == 0L) { alertColor } else { normalColor })
    }

    fun formatCardGoals(team: BingoTeam?, text: TextProvider): MutableText {
        val card = cards.find { it.id == team?.cardId } ?: cards.first()
        val remainingCards = (options.winCondition as? BingoWinCondition.Cards)
            ?.cards
            ?.minus(1)
            ?.minus(team?.countCards() ?: 0)
            ?.takeIf { it > 0 }

        return card.options.goal.format(text)
            .also {
                if (remainingCards != null)
                    it.append(Text.literal(" +$remainingCards").formatted(Formatting.BOLD))
                if (options.winCondition is BingoWinCondition.Infinite)
                    it.append(Text.literal(" + ∞"))
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
        previewMap = null
        // [ScoredItemCheck] calls createInitialCards when entering PREGAME, so this card should be immediately replaced
        cards.clear()
        gameOverInfo = null
        isForfeit = false
        menu.page = MenuPage.ROOT
    }

}