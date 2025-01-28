package me.jfenn.bingo.common.scope

import me.jfenn.bingo.api.BingoApi
import me.jfenn.bingo.api.BingoEvents
import me.jfenn.bingo.api.IBingoApi
import me.jfenn.bingo.common.MOD_ID
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.commonInit
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.lobbyWorld
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.utils.EventListener
import me.jfenn.bingo.platform.IPersistentStateManager
import me.jfenn.bingo.platform.IServerEvents
import me.jfenn.bingo.platform.event.model.ReloadEvent
import me.jfenn.bingo.platform.getFromWorld
import me.jfenn.bingo.platform.scope.IScopeManager
import me.jfenn.bingo.platform.utils.IEventListener
import net.minecraft.server.MinecraftServer
import org.koin.core.Koin
import org.koin.core.error.ScopeNotCreatedException
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeID
import org.slf4j.Logger

class ScopeManager(
    private val koin: Koin,
    private val log: Logger,
    private val persistentStateManager: IPersistentStateManager,
    private val configService: ConfigService,
    serverEvents: IServerEvents,
) : IScopeManager {

    private fun getScopeId(server: MinecraftServer): ScopeID =
        System.identityHashCode(server).toString()

    override fun getScope(server: MinecraftServer?) : Scope? {
        server ?: return null
        return try {
            koin.getScope(getScopeId(server))
        } catch (e: ScopeNotCreatedException) {
            log.error("getScope invoked, but the server scope does not exist!")
            null
        }
    }

    override val onCreate: IEventListener<Scope> = EventListener()
    override val onCleanup: IEventListener<Scope> = EventListener()

    init {
        serverEvents.onServerStarted { server ->
            val isLobbyMode = server.lobbyWorld != null
            if (isLobbyMode) log.info("[ScopeManager] $MOD_ID.zip datapack exists - starting with isLobbyMode=true")
            else log.info("[ScopeManager] $MOD_ID.zip datapack does not exist - starting with isLobbyMode=false")

            // obtain the bingo game state from the save file
            val state = persistentStateManager.getFromWorld<BingoState>(
                world = server.overworld,
                id = BINGO_STATE_ID,
                default = { BingoState(options = configService.options) },
            )
            state.isLobbyMode = isLobbyMode

            log.info("[ScopeManager] Starting server scope...")
            val ctx = BingoScope(server, state)
            val scopeId = getScopeId(server)
            val scope = koin.createScope<BingoScope>(scopeId, ctx)
            scope.commonInit()

            val events = scope.get<ScopedEvents>()
            scope.registerCallback(events)
            events.onDataReload(ReloadEvent(server, server.resourceManager))

            // re-run state entry listeners
            if (state.state == GameState.UNINITIALIZED) {
                state.changeState(events, GameState.PREGAME)
            } else {
                events.onStateChange(state.state to state.state)
            }

            // If the state somehow does not have any cards (save error), fix that
            if (state.cards.isEmpty()) {
                log.error("[ScopeManager] There were no bingo cards in the save! Your game might not have shut down correctly, or your save data was corrupted.")
                scope.get<CardService>().createInitialCards()
            }

            val api = scope.get<IBingoApi>();
            BingoApi.set(api)
            onCreate(scope)
            BingoEvents.INIT.invoke(api)
        }

        serverEvents.onServerStopped { server ->
            val scope = getScope(server) ?: return@onServerStopped
            log.info("[ScopeManager] Closing server scope...")
            configService.writeOptions(scope.get())
            BingoApi.set(null)
            onCleanup(scope)
            BingoEvents.CLOSE.invoke(null)
            scope.close()
        }
    }

    companion object {
        const val BINGO_STATE_ID = MOD_ID
    }

}