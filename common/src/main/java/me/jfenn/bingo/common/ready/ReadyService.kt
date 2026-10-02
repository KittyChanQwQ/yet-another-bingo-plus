package me.jfenn.bingo.common.ready

import me.jfenn.bingo.api.state.GameState
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.JoinCommand
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.seconds
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.integrations.permissions.IPermissionsApi
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.IPlayerManager
import org.slf4j.Logger

internal class ReadyService(
    private val state: BingoState,
    private val teamService: TeamService,
    private val config: BingoConfig,
    private val readyTimerState: ReadyTimerState,
    private val playerManager: IPlayerManager,
    private val permissions: IPermissionsApi,
    private val text: TextProvider,
    private val log: Logger,
) {
    fun currentVote(): VoteType? {
        return readyTimerState.leadingVote() ?: when (state.state) {
            GameState.PREGAME -> VoteType.START
            GameState.POSTGAME -> VoteType.NEXT_ROUND
            else -> null
        }
    }

    fun isReadyEnabled(player: IPlayerHandle): Boolean {
        val isLobbyMode = state.isLobbyMode
        val isPregameCountdown = state.state == GameState.PREGAME && config.startWhenReadySeconds != null
        val isPostgameCountdown = state.state == GameState.POSTGAME && config.nextRoundWhenReadySeconds != null
        val isPermission = permissions.hasPermission(player, Permission.COMMAND_READY)
        return isLobbyMode && (isPregameCountdown || isPostgameCountdown) && isPermission
    }

    fun canUseReady(player: IPlayerHandle): Boolean {
        val isPostgameCountdown = state.state == GameState.POSTGAME && config.nextRoundWhenReadySeconds != null
        val isTeamChosen = !config.startWhenReadyWaitsForTeams ||
                playerManager.getPlayers().size == 1 ||
                teamService.isPlaying(player) ||
                state.playersSpectatingIds.contains(player.uuid) ||
                isPostgameCountdown
        return isReadyEnabled(player) && isTeamChosen
    }

    fun isFirstVoteSatisfied() = when (state.state) {
        GameState.PREGAME -> !config.startWhenReadyWaitsForFirstVote
        GameState.POSTGAME -> !config.nextRoundWhenReadyWaitsForFirstVote
        else -> false
    } || playerManager.getPlayers().any { readyTimerState.isReady(it.uuid) }

    fun isConditionSatisfied(): Boolean = when (state.state) {
        GameState.PREGAME -> {
            val players = playerManager.getPlayers()
            val isTeams = !config.startWhenReadyWaitsForTeams || players
                .filterNot { state.playersSpectatingIds.contains(it.player.uuid) }
                .all { teamService.isPlaying(it) }

            isTeams && players.size > 1 && state.options.isValid()
        }
        GameState.POSTGAME -> true
        else -> false
    }

    fun startTimer() {
        val duration = when (state.state) {
            GameState.PREGAME -> config.startWhenReadySeconds
            GameState.POSTGAME -> config.nextRoundWhenReadySeconds
            else -> null
        }

        if (duration != null) {
            log.info("[ReadyService] Starting the ${state.state} timer for $duration seconds")
            readyTimerState.startTimer(duration.seconds)
        }
    }

    fun setReady(player: IPlayerHandle, voteType: VoteType?): ReadyResult {
        if (!canUseReady(player)) {
            return ReadyResult.Failed(
                text.string(
                    StringKey.CommandReadyNotOnATeam,
                    "${JoinCommand.JOIN_COMMAND} ${JoinCommand.SPECTATORS}"
                )
            )
        }

        if (voteType != null && !readyTimerState.isRunning && isConditionSatisfied()) {
            startTimer()
        }

        if (!readyTimerState.isRunning) {
            return ReadyResult.Failed(text.string(StringKey.CommandReadyNotRunning))
        }

        readyTimerState.setReady(player.uuid, voteType)
        if (permissions.hasPermission(player, Permission.CONFIGURE_GAME)) {
            // if the player is an operator, lock the next action to operators only
            readyTimerState.operatorVote = voteType

            // clear the votes of any other options
            playerManager.getPlayers()
                .filter { readyTimerState.getVote(it.uuid) != voteType }
                .forEach { readyTimerState.setReady(it.uuid, null) }
        }

        playerManager.updatePlayerListName(player)
        return ReadyResult.ChangedTo(
            vote = voteType,
            message = text.string(
                if (voteType != null) StringKey.CommandReadyTrue else StringKey.CommandReadyFalse,
                player.playerName
            )
        )
    }

    fun setReady(player: IPlayerHandle, isReady: Boolean): ReadyResult =
        setReady(player, if (isReady) currentVote() else null)

    fun toggleReady(player: IPlayerHandle): ReadyResult {
        val isReady = !readyTimerState.isReady(player.uuid)
        return setReady(player, isReady)
    }

    fun setVote(player: IPlayerHandle, voteType: VoteType? = currentVote()): ReadyResult {
        return setReady(player, voteType)
    }

    fun tick() {
        // If the game can start, begin the timer!
        if (!readyTimerState.isRunning && !readyTimerState.isCancelled && isFirstVoteSatisfied() && isConditionSatisfied()) {
            startTimer()
        }

        // If the game is no longer ready, reset the timer!
        if (readyTimerState.isRunning && !isConditionSatisfied()) {
            log.info("[ReadyService] Cancelling the ${state.state} timer, as players are not ready")
            reset()
        }
    }

    fun reset() {
        readyTimerState.reset()
        playerManager.getPlayers().forEach { player ->
            playerManager.updatePlayerListName(player)
        }
    }
}