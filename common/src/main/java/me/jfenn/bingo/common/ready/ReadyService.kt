package me.jfenn.bingo.common.ready

import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.IPlayerManager
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.integrations.permissions.IPermissionsApi

class ReadyService(
    private val state: BingoState,
    private val config: BingoConfig,
    private val readyTimerState: ReadyTimerState,
    private val playerManager: IPlayerManager,
    private val permissions: IPermissionsApi,
) {
    fun canUseReady(player: IPlayerHandle): Boolean {
        val isLobbyMode = state.isLobbyMode
        val isPregameCountdown = state.state == GameState.PREGAME && config.startWhenReadySeconds != null
        val isPostgameCountdown = state.state == GameState.POSTGAME && config.nextRoundWhenReadySeconds != null
        val isPermission = permissions.hasPermission(player, Permission.COMMAND_READY)
        return isLobbyMode && (isPregameCountdown || isPostgameCountdown) && isPermission
    }

    fun setReady(player: IPlayerHandle, isReady: Boolean) {
        if (!canUseReady(player)) return
        readyTimerState.setReady(player.uuid, isReady)
        playerManager.updatePlayerListName(player)
    }

    fun toggleReady(player: IPlayerHandle) {
        setReady(player, !readyTimerState.isReady(player.uuid))
    }
}