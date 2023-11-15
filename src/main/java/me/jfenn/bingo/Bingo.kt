package me.jfenn.bingo

import kotlinx.datetime.Clock
import me.jfenn.bingo.integrations.*
import me.jfenn.bingo.integrations.MapItemHandler
import me.jfenn.bingo.map.MapRenderService
import me.jfenn.bingo.menu.MenuService
import me.jfenn.bingo.state.BingoContext
import me.jfenn.bingo.state.BingoState
import me.jfenn.bingo.state.GameState
import me.jfenn.bingo.state.SerializedPersistentState
import me.jfenn.bingo.utils.ContextEventListener
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

const val MOD_ID = "bingo"

val LOBBY_WORLD_ID = Identifier.of("bingo", "lobby")

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
        MotdManager.init()
        BingoBossBar.init()
        BingoScoreboard.init()
        PlayerManager.init()
        GameRuleManager.init()
        ScoreManager.init()
        TimerCheck.init()
        MapItemHandler.init()
        BingoCardManager.init()

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

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register { message, player, type ->
            val (server, state) = getContext(player.server) ?: return@register true
            if (state.state != GameState.PLAYING) return@register true
            val team = player.scoreboardTeam ?: return@register true

            val messageText = Text.empty()
                .append(Text.literal("TEAM ${player.entityName}").formatted(team.color))
                .append(Text.literal(": "))
                .append(message.content)

            // broadcast message to team
            for (otherPlayer in server.playerManager.playerList) {
                if (otherPlayer.isTeamPlayer(team)) otherPlayer.sendMessage(messageText)
            }

            false
        }

        CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, environment ->
            for (command in commands()) {
                dispatcher.register(command)
            }
        }

        ServerPlayConnectionEvents.DISCONNECT.register { _, server ->
            val (_, state) = getContext(server) ?: return@register
            // if the last player is disconnecting after a bingo game, shut down the server
            if (state.config.autoShutDownInPostgame && state.state == GameState.POSTGAME && server.currentPlayerCount <= 1) {
                log.info("Last player has disconnected and 'autoShutDownInPostgame' is true; Shutting down the server...")
                server.stop(false)
            }
        }
    }

}
