package me.jfenn.bingo.common.scoring

import kotlinx.datetime.Instant
import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.card.objective.BingoObjectiveManager
import me.jfenn.bingo.common.config.BingoCardOptions
import me.jfenn.bingo.common.config.BingoGoal
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.game.GameEndReason
import me.jfenn.bingo.common.game.GameService
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.BingoTeam
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
        val prevLeadingTeam = ScoreService.getLeading(state, card)
        val rootObjectives = card.entries.mapNotNull { card.objectives[it.objectiveId] }

        // find teams where any card objective has been modified since the last tick
        val teams = state.getRegisteredTeams()
            // if the team is already a winner, their score should not change
            .filter { !it.isWinner() }

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

        // check if a team has won the game
        for (team in teams) {
            val isWinner = when (val goal = card.options.goal) {
                is BingoGoal.Items -> team.score.items >= goal.items
                is BingoGoal.Lines -> team.score.lines >= goal.lines
            }

            if (isWinner && options.isRankedMode && sendUpdates) {
                // announce that another team has completed their card
                scoreUpdateService.sendCardCompleted(team)
            }

            if (isWinner) {
                // end the card, since a team has won
                finishCard(team, card)
                return
            }
        }

        // check if the card is in a stalemate
        if (ScoreService.isLockout(state, card)) {
            endCard(card, GameEndReason.LOCKOUT)
            return
        }

        // check if the leading team has changed, and announce the leader
        val leadingTeam = ScoreService.getLeading(state, card)
        if (prevLeadingTeam != leadingTeam && options.showLeadingTeam && state.getRegisteredTeams().size > 1 && sendUpdates) {
            // if the leading team should be shown, announce the new leader
            if (leadingTeam != null) {
                scoreUpdateService.sendTeamLeading(leadingTeam)
            } else {
                teams.firstOrNull()?.let {
                    scoreUpdateService.sendTeamTied(it)
                }
            }
        }
    }

    private fun endCard(card: BingoCard, endReason: GameEndReason) {
        val teams = state.getRegisteredTeams()
            .filter { it.card == card.id }

        for (team in teams) {
            finishCard(team, card, endReason)
        }
    }

    private fun finishCard(team: BingoTeam, card: BingoCard, endReason: GameEndReason = GameEndReason.BINGO) {
        val nextCard = state.cards.find { it.id == card.nextCardId }
            // if in infinite mode, keep creating new cards
            ?: if (options.isInfiniteMode) {
                cardService.newCardTail(
                    card.options.takeIf { it.isValid() }?.copy()
                        ?: BingoCardOptions()
                )
            } else null

        if (options.isRankedMode) {
            // in ranked mode, the winning team immediately moves onto the next card
            team.card = nextCard?.id
        } else if (nextCard == null) {
            // if we run out of cards (and are not in ranked mode), end the game
            gameService.end(endReason)
        } else {
            // otherwise, move all teams on this card to the next
            val teams = state.getRegisteredTeams()
                .filter { it.card == card.id }

            teams.forEach { it.card = nextCard.id }
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
                val cards = state.getRegisteredTeams()
                    .mapNotNull { state.getCard(it) }
                    .distinct()

                for (card in cards) {
                    tickCard(card)
                }
            }
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