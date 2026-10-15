package me.jfenn.bingo.common.scoring

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.jfenn.bingo.api.options.BingoGoal
import me.jfenn.bingo.api.options.BingoWinCondition
import me.jfenn.bingo.api.options.EndWhen
import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.objective.BingoObjective
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.team.TeamScore
import me.jfenn.bingo.common.team.TeamState
import java.time.Instant

@Serializable
internal sealed interface StalemateReason {
    interface Win : StalemateReason { val team: BingoTeamKey }
    @SerialName("win_by_stalemate")
    data class WinByStalemate(override val team: BingoTeamKey) : StalemateReason, Win
    @SerialName("win_by_lockout")
    data class WinByLockout(override val team: BingoTeamKey) : StalemateReason, Win
    @SerialName("stalemate")
    data object Stalemate : StalemateReason
    @SerialName("lockout")
    data object Lockout : StalemateReason
    @SerialName("impossible_goal")
    data object ImpossibleGoal: StalemateReason
}

internal object ScoreService {

    fun isStalemate(state: BingoState, card: BingoCard): StalemateReason? {
        // select only teams on the card
        val teams = state.getRegisteredTeams()
            .filter { it.isPlaying() && it.cardId == card.id }

        // if there are no teams, then I have no idea what's going on
        if (teams.isEmpty()) return null

        // if the win condition is "ReplaceGoals", then any impossible goals could get replaced by another team
        // (and since the score is accumulated, this probably isn't a soft-lock anyway)
        // TODO: there probably _are_ stalemate conditions in ReplaceGoals, but they'll be much more specific
        if (state.options.winCondition is BingoWinCondition.ReplaceGoals)
            return null

        val leadingTeam = getLeading(state)?.takeIf { it.cardId == card.id }

        if (
            card.options.isLockoutMode && card.entries
                .mapNotNull { card.objectives[it.objectiveId] }
                .all { it.hasAnyAchieved() }
        ) {
            // if the entire card is full, then it is a lockout (regardless of the goal)
            return if (leadingTeam != null) StalemateReason.WinByLockout(leadingTeam.key) else StalemateReason.Lockout
        }

        val cardGoal = card.options.goal
        val comparator = when (cardGoal) {
            is BingoGoal.Items -> compareBy { it.items }
            is BingoGoal.Lines -> compareBy<TeamScore> { it.lines }.thenBy { it.items }
        }

        val leadingTeamOrTie = teams.maxWithOrNull { a, b -> comparator.compare(a.score, b.score) }

        // Determines whether [team] can still capture [objective]. This is used to decide whether
        // the game should end because nobody can win anymore, so it deliberately errs towards
        // "not possible" - a tile is only considered achievable if it (and enough of its required
        // sub-objectives) can still be completed.
        //
        // [isTile] is true only for the top-level objectives on the card; sub-objectives (the
        // contents of one_of/all_of) are not locked by lockout mode, so the lockout check is
        // skipped when recursing into them.
        fun isPossible(
            team: BingoTeam,
            objective: BingoObjective,
            isTile: Boolean = true,
            depth: Int = 0,
        ): Boolean {
            // if the team has already achieved the objective, it still counts towards their score
            if (objective.hasTeamAchieved(team.key)) return true

            // a permanent inverse objective can no longer be achieved once it has been lost
            if (objective is BingoObjective.InverseEntry && objective.permanent)
                return false

            // in lockout mode, a tile that has already been claimed by another team can never be re-claimed
            if (isTile) {
                val canLoseTile = card.options.isInventoryMode
                val isPermanentlyLocked = card.options.isLockoutMode && !canLoseTile && objective.hasAnyAchieved()
                if (isPermanentlyLocked) return false
            }

            if (depth >= 10) return true

            // recurse into composed objectives - a tile is only achievable if enough of its
            // required sub-objectives are still achievable. Without this, a tile whose contents
            // have been locked/lost would still be counted as "possible" and the game would run
            // forever instead of ending.
            return when (objective) {
                is BingoObjective.SomeOfEntry -> {
                    val children = objective.someOfObjectives.mapNotNull { card.objectives[it] }
                    if (children.isEmpty()) {
                        false
                    } else {
                        val required = objective.valueMin.coerceIn(1, children.size)
                        children.count { isPossible(team, it, isTile = false, depth = depth + 1) } >= required
                    }
                }
                else -> true
            }
        }

        // if noone can beat or tie with the leading team, then it is a lockout (the leading team has won)
        val isStalemate = leadingTeamOrTie != null && teams
            // for all teams other than the leading team...
            .filter { it.key != leadingTeamOrTie.key }
            .takeIf { it.isNotEmpty() }
            // at least one team can tie or beat the leading score
            ?.none { team ->
                val possibleLines = card.countLines { objective -> isPossible(team, objective) }
                val possibleItems = card.countItems { objective -> isPossible(team, objective) }
                val possibleScore = TeamScore(
                    cards = 0,
                    lines = possibleLines,
                    items = possibleItems,
                )
                // if a team can capture more tiles, and it would either tie or beat the current team
                comparator.compare(possibleScore, team.currentScore) > 0 && comparator.compare(
                    possibleScore,
                    leadingTeamOrTie.currentScore
                ) >= 0
            } == true

        fun isGoalPossible(team: BingoTeam): Boolean {
            return when (cardGoal) {
                is BingoGoal.Items -> {
                    val possibleItems = card.countItems { entry -> isPossible(team, entry) }
                    possibleItems >= cardGoal.items
                }
                is BingoGoal.Lines -> {
                    val possibleLines = card.countLines { entry -> isPossible(team, entry) }
                    // if no team can capture the amount of lines needed for the win condition
                    possibleLines >= cardGoal.lines
                }
            }
        }

        val endWhen = state.options.endGameWhen
        val isGoalImpossible = when {
            // if the goal is "full card" when isLockoutMode=true, disregard any unreachable goals
            // (otherwise the game would end instantly)
            cardGoal.isFullCard() && card.options.isLockoutMode && teams.size > 1 -> false
            // if no team can capture the amount of items/lines needed for the win condition
            else -> {
                // figure out the number of teams that are required to complete the game
                val requiredTeams = when (endWhen) {
                    is EndWhen.Never,
                    is EndWhen.FirstWin -> 1
                    is EndWhen.TeamsWin -> endWhen.teams
                    is EndWhen.AllWin -> teams.size
                }

                teams.count { isGoalPossible(it) } < requiredTeams
            }
        }

        return when {
            // If there is a leading team when a stalemate is triggered, that team should win the card
            isStalemate && !isGoalImpossible && leadingTeam != null -> StalemateReason.WinByStalemate(leadingTeam.key)
            isGoalImpossible -> StalemateReason.ImpossibleGoal
            isStalemate -> StalemateReason.Stalemate
            else -> null
        }
    }

    private val compareByCompletion = compareByDescending<BingoTeam> {
        (it.state as? TeamState.Finished)?.completedAt ?: Instant.MAX
    }

    private val compareByCards = compareByCompletion.thenBy { it.score.cards }
    private val compareByLines = compareByCards.thenBy { it.score.lines }
    private val compareByItems = compareByLines.thenBy { it.score.items}

    val comparator = compareByItems

    fun getLeading(
        state: BingoState,
        allowTies: Boolean = false,
        comparator: Comparator<BingoTeam> = ScoreService.comparator
    ): BingoTeam? {
        var ret: BingoTeam? = null
        var leadingTeam: BingoTeam? = null
        for (team in state.getRegisteredTeams()) {
            val comparison = leadingTeam?.let { comparator.compare(team, it) }
            if (comparison == null || comparison > 0) {
                ret = team
                leadingTeam = team
            } else if (comparison == 0 && !allowTies) {
                ret = null
            }
        }
        return ret
    }

    fun getLeadingByCompletion(state: BingoState) = getLeading(state, comparator = compareByCompletion)

    fun getLeadingByCards(state: BingoState, allowTies: Boolean = false) = getLeading(state, allowTies, compareByCards)

    fun getLeadingByLines(state: BingoState, allowTies: Boolean = false) = getLeading(state, allowTies, compareByLines)

    fun getLeadingByItems(state: BingoState, allowTies: Boolean = false) = getLeading(state, allowTies, compareByItems)

}