package me.jfenn.bingo.common.autorestart

import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.platform.IExecutors
import me.jfenn.bingo.platform.IPlayerManager
import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import org.slf4j.Logger

class RestartOnLeaveController(
    private val log: Logger,
    private val config: BingoConfig,
    private val state: BingoState,
    private val resetService: ResetService,
    private val server: MinecraftServer,
    private val playerManager: IPlayerManager,
    private val serverTaskExecutor: IExecutors.IServerTaskExecutor,
    events: ScopedEvents,
) {
    init {
        events.onPlayerDisconnect {
            // This should only ever run on the server env
            if (FabricLoader.getInstance().environmentType != EnvType.SERVER) return@onPlayerDisconnect

            // if the last player is disconnecting after a bingo game, shut down the server
            if (server.isRunning && config.nextRoundWhenEveryoneDisconnects && state.isLobbyMode && state.state == GameState.POSTGAME && playerManager.getPlayers().isEmpty()) {
                log.info("[RestartOnLeaveController] Last player has disconnected and 'nextRoundWhenEveryoneDisconnects' is true; Resetting...")
                serverTaskExecutor.execute {
                    resetService.resetGame()
                }
            }
        }
    }
}