package me.jfenn.bingo.common

import me.jfenn.bingo.common.commands.*
import me.jfenn.bingo.common.integrations.*
import me.jfenn.bingo.common.map.MapRenderService
import me.jfenn.bingo.common.menu.MenuController
import me.jfenn.bingo.common.state.BingoContext
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.utils.ContextEventListener
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

const val MOD_ID = "bingo"

val LOBBY_WORLD_ID = Identifier.of("bingo", "lobby")!!

object Bingo : ModInitializer {

    val log = LoggerFactory.getLogger(this::class.java)

    // States are stored by the identity hash code of the MinecraftServer instance which they relate to
    // - this ensures no cross-server pollution when running the mod client-side
    private val contexts = mutableMapOf<Int, BingoContext>()

    fun getContext(server: MinecraftServer?): BingoContext? =
        server?.let { contexts[System.identityHashCode(it)] }

    override fun onInitialize() {
        log.info("$MOD_ID mod initialized (main)")
        MapRenderService.validateItems()

        BingoCommands.init()
        JoinCommand.init()
        ChatCommand.init()
        CoordsCommand.init()
        RestartCommand.init()

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
