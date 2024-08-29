package me.jfenn.bingo.common.scoring

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jfenn.bingo.common.card.objective.BingoObjectiveManager
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.game.GameEndReason
import me.jfenn.bingo.common.game.GameService
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState

/**
 * Searches for items in each player's inventory that can
 * satisfy a tile on the card, and updates the score if found.
 */
class ScoredItemCheck(
    events: ScopedEvents,
    private val options: BingoOptions,
    private val state: BingoState,
    private val gameService: GameService,
    private val scoreUpdateService: ScoreUpdateService,
    private val objectiveManager: BingoObjectiveManager,
) : BingoComponent() {

    private fun updateScores(
        tickStart: Instant,
        sendUpdates: Boolean,
    ) {
        val prevLeadingTeam = ScoreService.getLeading(state)
        val rootObjectives = state.card.entries.mapNotNull { state.card.objectives[it.objectiveId] }

        // find teams where any card objective has been modified since the last tick
        val updatedTeams = state.getRegisteredTeams()
            .filter { team -> rootObjectives.maxOf { it.updatedAt(team.key) } > tickStart }

        // if there are no updated teams, we can skip the remaining checks...
        if (updatedTeams.isEmpty()) return

        for (team in updatedTeams) {
            // iterate through all updated objectives on the card
            rootObjectives
                .takeIf { sendUpdates }
                // if the objective has been updated in this tick
                ?.filter { it.updatedAt(team.key) > tickStart }
                ?.forEach { objective ->
                    if (objective.hasAchieved(team.key)) {
                        // find the player that captured the objective
                        val player = objective.players
                            .mapNotNull { (uuid, instant) ->
                                team.players.find { it.uuid == uuid }?.let { it to instant }
                            }
                            .maxByOrNull { (_, capture) -> capture.instant }
                            ?.first

                        scoreUpdateService.sendItemCaptured(
                            scoredPlayer = player,
                            team = team,
                            objective = objective,
                        )
                    } else {
                        scoreUpdateService.sendItemLost(team)
                    }
                }

            // update the score for each modified team
            val scoreBefore = team.score
            val scoreAfter = state.updateScore(team)

            // if the team has scored a line, post a message
            if (scoreAfter.lines > scoreBefore.lines && sendUpdates) {
                scoreUpdateService.sendLineCaptured(team)
            }
        }

        // check if a team has won the game
        for (team in updatedTeams) {
            val isWinner = state.updateWinner(team)

            if (isWinner && options.isRankedMode && sendUpdates) {
                // announce that another team has completed their card
                scoreUpdateService.sendCardCompleted(team)
            }

            if (isWinner && !options.isRankedMode) {
                // end the game, since a team has won
                gameService.end(GameEndReason.BINGO)
                return
            }
        }

        // check if the game is in a stalemate
        if (ScoreService.isLockout(state)) {
            gameService.end(GameEndReason.LOCKOUT)
            return
        }

        // check if the leading team has changed, and announce the leader
        val leadingTeam = ScoreService.getLeading(state)
        if (prevLeadingTeam != leadingTeam && options.showLeadingTeam && state.getRegisteredTeams().size > 1 && sendUpdates) {
            // if the leading team should be shown, announce the new leader
            if (leadingTeam != null) {
                scoreUpdateService.sendTeamLeading(leadingTeam)
            } else {
                updatedTeams.firstOrNull()?.let {
                    scoreUpdateService.sendTeamTied(it)
                }
            }
        }
    }

    init {
        events.onGameTick {
            // Initialize objectives
            if (!state.card.isInitialized) {
                // No, this isn't a bug...
                // init() needs to be called multiple times to populate display info on dependent objectives
                objectiveManager.init(state.card)
                objectiveManager.init(state.card)
                objectiveManager.init(state.card)

                state.card.isInitialized = true
            }

            if (state.state == GameState.PLAYING) {
                val tickStart = Clock.System.now()

                // Update all objectives
                objectiveManager.tick(state.card)

                updateScores(
                    tickStart = tickStart,
                    sendUpdates = true,
                )
            }
        }

        events.onEnter(GameState.PLAYING) {
            // Run one tick when the game starts
            // - this prevents notification spam for tiles that the player has already captured at the start of the game
            objectiveManager.tick(state.card)
            objectiveManager.tick(state.card)
            objectiveManager.tick(state.card)

            updateScores(
                tickStart = Instant.DISTANT_PAST,
                sendUpdates = false,
            )
        }

        events.onEnter(GameState.POSTGAME) {
            // Mark all goals as "seen" when game ends
            // (if hidden items is true, players should see all items after the game)
            val teamKeys = state.teams.keys
            for (goal in state.card.objectives.values) {
                goal.teamsSeen = teamKeys.toMutableSet()
            }
        }
    }

}