package me.jfenn.bingo.common.scoring

import kotlinx.datetime.Instant
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
import me.jfenn.bingo.common.team.TeamCompletedCard
import me.jfenn.bingo.common.team.TeamWinner
import org.slf4j.Logger

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
    events: ScopedEvents,
) : BingoComponent() {

    private fun updateScores(
        card: BingoCard,
        tickStart: Instant,
        sendUpdates: Boolean,
    ) {
        val rootObjectives = card.entries.mapNotNull { card.objectives[it.objectiveId] }

        val teams = state.getRegisteredTeams()
            // if the team is already a winner, their score should not change
            .filter { !it.isWinner() && it.cardId == card.id }

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
                onCardCompleted(team, card, true)

                if (options.skipCardWhenLost) {
                    teams.filter { it.cardId == card.id }
                        .forEach { onCardCompleted(it, card, false) }
                }

                break
            }
        }

        // check if any team has won the game
        if (options.endGameWhenComplete && teams.any { it.isWinner() }) {
            gameService.end(GameEndReason.BINGO)
        }

        // skip the card if it is in a stalemate
        if (options.skipCardWhenStalemate && ScoreService.isLockout(state, card)) {
            if (options.endGameWhenComplete && options.winCondition == BingoWinCondition.Cards(1)) {
                // if there's only one card, end the game when there's a lockout
                gameService.end(GameEndReason.LOCKOUT)
            } else {
                // otherwise, skip to the next card when a lockout occurs
                teams.filter { it.cardId == card.id }
                    .forEach { onCardCompleted(it, card, false) }
            }
        }
    }

    private fun onCardCompleted(team: BingoTeam, card: BingoCard, isWinner: Boolean) {
        val now = state.updatedAt ?: Instant.DISTANT_PAST

        team.completedCards += TeamCompletedCard(
            id = card.id,
            completedAt = now,
            isWinner = isWinner,
            score = team.score,
        )

        val condition = options.winCondition
        if (condition is BingoWinCondition.Cards && team.countCards() >= condition.cards) {
            // the team has won the game
            team.winner = TeamWinner(now)
            // add to playersJoinedIds - tells [PlayerController] to run updateGameMode
            // which should place winning teams in spectator
            state.playersJoinedIds += team.players.map { it.uuid }
        } else {
            team.cardId = card.nextCardId
        }

        if (isWinner && !options.endGameWhenComplete) {
            // announce that another team has completed their card
            scoreUpdateService.sendCardCompleted(team)
        }
    }

    private fun tickCard(card: BingoCard) {
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
        // check if the leading team has changed, and announce the leader
        val leadingTeam = ScoreService.getLeading(state)
        if (prevLeadingTeam != leadingTeam && options.showLeadingTeam && state.getRegisteredTeams().size > 1) {
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

        val teams = state.getRegisteredTeams()
            // if the team is already a winner, their score should not change
            .filter { !it.isWinner() }

        // if a team overruns / is missing a card, assign one!
        val newCard by lazy {
            val card = state.cards.last()
            cardService.newCardTail(card.options)
        }

        for (team in teams) {
            if (team.cardId == null || state.cards.none { it.id == team.cardId }) {
                team.cardId = newCard.id
            }
        }
    }

    init {
        events.onGameTick {
            for (card in state.cards) {
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

            if (state.state == GameState.PLAYING) {
                val prevLeadingTeam = ScoreService.getLeading(state)

                val cards = state.getRegisteredTeams()
                    .mapNotNull { state.getCard(it) }
                    .distinct()

                for (card in cards) {
                    tickCard(card)
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
    }

}