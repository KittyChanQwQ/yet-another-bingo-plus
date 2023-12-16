package me.jfenn.bingo.common

import kotlinx.datetime.Clock
import me.jfenn.bingo.common.commands.*
import me.jfenn.bingo.common.integrations.*
import me.jfenn.bingo.common.map.MapRenderService
import me.jfenn.bingo.common.menu.MenuService
import me.jfenn.bingo.common.state.BingoContext
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.state.SerializedPersistentState
import me.jfenn.bingo.common.utils.ContextEventListener
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

const val MOD_ID = "bingo"

val LOBBY_WORLD_ID = Identifier.of("bingo", "lobby")!!

object Bingo : ModInitializer {

    private val log = LoggerFactory.getLogger(this::class.java)

    // States are stored by the identity hash code of the MinecraftServer instance which they relate to
    // - this ensures no cross-server pollution when running the mod client-side
    private val contexts = mutableMapOf<Int, BingoContext>()

    fun getContext(server: MinecraftServer): BingoContext? = contexts[System.identityHashCode(server)]

    private fun setContext(server: MinecraftServer, state: BingoState): BingoContext {
        val ctx = BingoContext(server, state)
        contexts[System.identityHashCode(server)] = ctx
        return ctx
    }

    val onGameTick = ContextEventListener<BingoContext, Unit>()
    val onUpdateTick = ContextEventListener<BingoContext, Unit>()

    override fun onInitialize() {
        log.info("$MOD_ID mod initialized (main)")
        MapRenderService.validateItems()

        MenuService.init()
        MotdController.init()
        BossBarController.init()
        ScoreboardController.init()
        PlayerController.init()
        GameRuleController.init()
        BingoCardController.init()

        ScoredItemCheck.init()
        TimerCheck.init()

        MapItemHandler.init()

        BingoCommands.init()
        JoinCommand.init()
        ChatCommand.init()
        CoordsCommand.init()
        RestartCommand.init()

        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            val lobbyWorld = server.worlds.find { it.registryKey.value == LOBBY_WORLD_ID } ?: run {
                log.error("Lobby world dimension $LOBBY_WORLD_ID not found, exiting...")
                return@register
            }

            // obtain the bingo game state from the save file
            val state: BingoState = SerializedPersistentState.fromWorld(lobbyWorld)

            // if the game was previously running, increment its updated timestamp by the time spent offline
            // (this is so that the timer only counts the amount of time spent in-game or with the server running)
            state.updatedAt?.let { lastUpdate ->
                state.timeOffline += (Clock.System.now() - lastUpdate)
            }

            val ctx = setContext(server, state)

            // re-run state entry listeners
            state.state.onEnter(ctx, state.state)
        }

        ServerTickEvents.START_SERVER_TICK.register { server ->
            val ctx = getContext(server) ?: return@register
            onGameTick(ctx, Unit)

            if (server.ticks % 20 == 0) {
                onUpdateTick(ctx, Unit)
            }
        }

        ServerPlayConnectionEvents.DISCONNECT.register { _, server ->
            val (_, state) = getContext(server) ?: return@register
            // if the last player is disconnecting after a bingo game, shut down the server
            if (server.isRunning && state.config.autoShutDownInPostgame && state.state == GameState.POSTGAME && server.currentPlayerCount <= 1) {
                log.info("Last player has disconnected and 'autoShutDownInPostgame' is true; Shutting down the server...")
                server.stop(false)
            }
        }
    }

}
