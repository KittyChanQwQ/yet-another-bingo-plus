package me.jfenn.bingo.common.scoring

import kotlinx.datetime.Instant
import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.card.objective.BingoObjectiveManager
import me.jfenn.bingo.common.config.BingoGoal
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.BingoWinCondition
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.game.GameEndReason
import me.jfenn.bingo.common.game.GameService
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.team.TeamCompletedCard
import me.jfenn.bingo.common.team.TeamWinner
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.util.Formatting
import org.slf4j.Logger
import java.util.*
import kotlin.random.Random

/**
 * Searches for items in each player's inventory that can
 * satisfy a tile on the card, and updates the score if found.
 */
class ScoredItemCheck(
    private val log: Logger,
    private val options: BingoOptions,
    private val state: BingoState,
    private val gameService: GameService,
    private val cardService: CardService,
    private val scoreUpdateService: ScoreUpdateService,
    private val objectiveManager: BingoObjectiveManager,
    private val playerManager: IPlayerManager,
    private val text: TextProvider,
    events: ScopedEvents,
) : BingoComponent() {

    // Card completed packets should only be sent if the game continues and does not end
    // - these are stored in a map temporarily and are sent/cleared at the end of each tick
    private val pendingCardCompletedPackets = mutableMapOf<BingoTeamKey, CardCompletedPacket>()

    private fun updateScores(
        card: BingoCard,
        tickStart: Instant,
        sendUpdates: Boolean,
    ) {
        val rootObjectives = card.entries.mapNotNull { card.objectives[it.objectiveId] }

        val teams = state.getRegisteredTeams()
            // if the team is already a winner, their score should not change
            .filter { it.isPlaying() && it.cardId == card.id }

        for (team in teams) {
            // iterate through all updated objectives on the card
            rootObjectives
                .takeIf { sendUpdates }
                // if the objective has been updated in this tick
                ?.filter { it.updatedAt(team.key) == tickStart }
                ?.forEach { objective ->
                    if (objective.hasAchieved(team.key)) {
                        // find the player that captured the objective
                        val player = objective.players
                            .mapNotNull { (uuid, instant) ->
                                team.players.find { it.uuid == uuid }?.let { it to instant }
                            }
                            .maxByOrNull { (_, capture) -> capture.instant }
                            ?.first

                        log.info("[ScoredItemCheck] ${player?.name ?: team.id} got ${objective.id}")

                        scoreUpdateService.sendItemCaptured(
                            scoredPlayer = player,
                            team = team,
                            objective = objective,
                            card = card,
                        )
                    } else {
                        scoreUpdateService.sendItemLost(team)
                    }
                }

            // update the score for each modified team
            val scoreBefore = team.score
            val scoreAfter = card.getTeamScore(team)

            if (scoreAfter != scoreBefore) {
                team.score = scoreAfter
                log.info("[ScoredItemCheck] Updated score for team {}: {}", team.id, scoreAfter)
            }

            // if the team has scored a line, post a message
            if (scoreAfter.lines > scoreBefore.lines && sendUpdates) {
                scoreUpdateService.sendLineCaptured(team)
            }
        }

        // check if a team has completed the card
        for (team in teams) {
            val isWinner = when (val goal = card.options.goal) {
                is BingoGoal.Items -> team.score.items >= goal.items
                is BingoGoal.Lines -> team.score.lines >= goal.lines
            }

            if (isWinner) {
                log.info("[ScoredItemCheck] Team {} has completed their card", team.id)
                onCardCompleted(team, card, true)

                if (options.skipCardWhenLost) {
                    teams.filter { it.cardId == card.id }
                        .forEach { onCardCompleted(it, card, false) }
                }

                break
            }
        }

        // skip the card if it is in a stalemate
        if (ScoreService.isLockout(state, card)) {
            if (options.endGameWhenStalemate) {
                log.info("[ScoredItemCheck] Ending the game because the card has reached a stalemate")
                // if there's only one card, end the game when there's a lockout
                gameService.end(GameEndReason.Lockout)
            } else {
                log.info("[ScoredItemCheck] Skipping the current card because it has reached a stalemate")
                // otherwise, skip to the next card when a lockout occurs
                teams.filter { it.cardId == card.id }
                    .forEach { onCardCompleted(it, card, false) }
            }
        }

        // we want ticks to stop at Int.MAX_VALUE so that it does not overflow into a negative
        if (card.ticks < Int.MAX_VALUE)
            card.ticks++
    }

    private fun onCardCompleted(team: BingoTeam, card: BingoCard, isWinner: Boolean) {
        val now = state.updatedAt ?: Instant.DISTANT_PAST

        team.completedCards += TeamCompletedCard(
            // Copy the card in its winning state
            card = card.copy().also { initCard(it) },
            completedAt = now,
            isWinner = isWinner,
            score = team.score,
        )

        val condition = options.winCondition
        if (condition is BingoWinCondition.Cards && team.countCards() >= condition.cards) {
            // the team has won the game
            team.winner = TeamWinner(now)
            team.cardId = null
            // add to playersJoinedIds - tells [PlayerController] to run updateGameMode
            // which should place winning teams in spectator
            state.playersJoinedIds += team.players.map { it.uuid }
        } else {
            // If the team is supposed to get a new card, check that it has >3 ticks
            // (this relates to init() calling the tick function 3 times)
            // - if the card is immediately achieved, we should end the game because this might be an infinite loop
            if (card.ticks <= 3) {
                val cardGoal = card.options.goal.format(text)
                val cardScore = when (card.options.goal) {
                    is BingoGoal.Items -> text.itemCount(team.score.items)
                    is BingoGoal.Lines -> text.lineCount(team.score.lines)
                }
                val message = text.string(StringKey.CommandStartWillImmediatelyEnd, cardGoal, cardScore).formatted(Formatting.RED)
                for (player in playerManager.getPlayers()) {
                    player.sendMessage(message)
                }

                log.error("[ScoredItemCheck] Ending the game because a card was completed immediately")
                log.error(" - This is done to avoid repeatedly creating new cards in a loop. You should check the 'Goal' settings to make sure that cards aren't completed as soon as the game starts.")

                state.isForfeit = true
                gameService.end(null)
                return
            }

            val nextCard = card.nextCardId?.let { state.getCard(it) }
            team.cardId = nextCard?.id

            if (nextCard != null) {
                onCardAssigned(nextCard, listOf(team))
            }
        }

        if (!options.endGameWhenComplete || condition != BingoWinCondition.Cards(1)) {
            // announce that another team has completed their card
            pendingCardCompletedPackets[team.key] = CardCompletedPacket(isWinner)
        }
    }

    private fun tickCard(card: BingoCard) {
        if (state.endedAt != null) return
        val tickStart = state.updatedAt ?: Instant.DISTANT_PAST

        // Update all objectives
        objectiveManager.tick(card)

        updateScores(
            card = card,
            tickStart = tickStart,
            sendUpdates = true,
        )
    }

    private fun postTickCards(
        prevLeadingTeam: BingoTeam?,
    ) {
        if (state.endedAt != null) return

        val cardCompletedPackets = pendingCardCompletedPackets.toList()
        pendingCardCompletedPackets.clear()

        val teams = state.getRegisteredTeams()

        // check if any team has won the game
        if (options.endGameWhenComplete) {
            if (teams.any { it.isWinner() }) {
                log.info("[ScoredItemCheck] Ending the game because a team has won")
                gameService.end(GameEndReason.Bingo)
                return
            }
        } else {
            if (teams.all { it.isWinner() }) {
                log.info("[ScoredItemCheck] Ending the game because all teams have finished")
                gameService.end(GameEndReason.Bingo)
                return
            }
        }

        // otherwise, send any pending card completed packets
        for ((teamKey, packet) in cardCompletedPackets) {
            val team = state.teams[teamKey] ?: continue
            scoreUpdateService.sendCardCompleted(team, packet.isWinner)
        }

        // check if the leading team has changed, and announce the leader
        val leadingTeam = ScoreService.getLeading(state)
        if (prevLeadingTeam != leadingTeam && options.showLeadingTeam && teams.size > 1) {
            // if the leading team should be shown, announce the new leader
            if (leadingTeam != null) {
                scoreUpdateService.sendTeamLeading(leadingTeam)
            } else {
                val tiedTeam = ScoreService.getLeading(state, allowTies = true)
                tiedTeam?.let {
                    scoreUpdateService.sendTeamTied(it)
                }
            }
        }

        // if a team overruns / is missing a card, assign one!
        val teamsWithoutCards = teams
            .filter { !it.isWinner() }
            .filter { team -> team.cardId == null || state.cards.none { it.id == team.cardId } }

        if (teamsWithoutCards.isNotEmpty()) {
            val newCard = cardService.generate(
                id = UUID.randomUUID(),
                seed = Random.nextLong(),
                cardOptions = state.cards.last().options.copy(),
                excludeObjectives = objectiveManager.listExcludedIds().toSet(),
            )

            state.pushCardTail(newCard)

            teamsWithoutCards.forEach {
                it.cardId = newCard.id
            }

            onCardAssigned(newCard, teamsWithoutCards)
        }
    }

    private fun initCard(card: BingoCard) {
        // Initialize objectives
        if (!card.isInitialized) {
            // No, this isn't a bug...
            // init() needs to be called multiple times to populate display info on dependent objectives
            objectiveManager.init(card)
            objectiveManager.init(card)
            objectiveManager.init(card)

            card.isInitialized = true
        }
    }

    private fun onCardAssigned(card: BingoCard, removeTeams: List<BingoTeam>) {
        initCard(card)

        for (team in removeTeams) {
            state.cards.forEach { it.removeTeam(team) }
        }

        // Run one tick when the game starts
        // - this prevents notification spam for tiles that the player has already captured at the start of the game
        objectiveManager.tick(card)
        objectiveManager.tick(card)
        objectiveManager.tick(card)

        updateScores(
            card = card,
            tickStart = Instant.DISTANT_PAST,
            sendUpdates = false,
        )
    }

    init {
        // Re-initialize completed cards when loading from a saved state
        for (team in state.getRegisteredTeams()) {
            for (completion in team.completedCards) {
                initCard(completion.card)
            }
        }

        events.onGameTick {
            // Team cards might have their own (copied) instances that don't get initialized right away
            state.getRegisteredTeams()
                .mapNotNull { state.getCard(it) }
                .map { initCard(it) }

            for (card in state.cards) {
                initCard(card)
            }

            if (state.state == GameState.PLAYING) {
                val prevLeadingTeam = ScoreService.getLeading(state)

                for (card in state.cards) {
                    val isActive = state.teams.values.any {
                        it.isPlaying() && it.cardId == card.id
                    }
                    if (isActive) {
                        tickCard(card)
                    }
                }

                postTickCards(prevLeadingTeam)
            }
        }

        events.onEnter(GameState.PREGAME) {
            // When entering PREGAME, generate initial cards from the config if they weren't already created
            cardService.createInitialCards()
        }

        events.onEnter(GameState.PLAYING) {
            for (card in state.cards) {
                onCardAssigned(
                    card = card,
                    removeTeams = emptyList(),
                )
            }
        }

        events.onEnter(GameState.POSTGAME) {
            // Mark all goals as "seen" when game ends
            // (if hidden items is true, players should see all items after the game)
            val teamKeys = state.teams.keys
            for (card in state.cards) {
                for (goal in card.objectives.values) {
                    goal.teamsSeen = teamKeys.toMutableSet()
                }
            }
        }

        events.onChangeTeam { player ->
            // Remove any of the player's held/scored items from the card
            for (card in state.cards) {
                card.removePlayer(player.uuid)
            }
        }
    }

}