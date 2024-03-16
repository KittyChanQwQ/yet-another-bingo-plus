package me.jfenn.bingo.common.autorestart

import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.scope.BingoKoin
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.Logger

class RestartOnLeaveController(
    private val log: Logger,
    private val config: BingoConfig,
) {
    init {
        ServerPlayConnectionEvents.DISCONNECT.register { _, server ->
            // This should only ever run on the server env
            if (FabricLoader.getInstance().environmentType != EnvType.SERVER) return@register
            val scope = BingoKoin.getScope(server) ?: return@register
            val state = scope.get<BingoState>()

            // if the last player is disconnecting after a bingo game, shut down the server
            if (server.isRunning && config.shutDownWhenEveryoneDisconnects && state.state == GameState.POSTGAME && server.currentPlayerCount <= 1) {
                log.info("[RestartOnLeaveController] Last player has disconnected and 'shutDownWhenEveryoneDisconnects' is true; Shutting down the server...")
                server.stop(false)
            }
        }
    }
}