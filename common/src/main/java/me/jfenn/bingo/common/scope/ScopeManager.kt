package me.jfenn.bingo.common.scope

import kotlinx.datetime.Clock
import me.jfenn.bingo.common.LOBBY_WORLD_ID
import me.jfenn.bingo.common.MOD_ID
import me.jfenn.bingo.common.commonInit
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.lobbyWorld
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.SerializedPersistentStateFactory
import me.jfenn.bingo.common.utils.EventListener
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.server.MinecraftServer
import org.koin.core.Koin
import org.koin.core.error.ScopeNotCreatedException
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeID
import org.slf4j.Logger

class ScopeManager(
    private val koin: Koin,
    private val log: Logger,
    private val persistentStateFactory: SerializedPersistentStateFactory,
) {

    private fun getScopeId(server: MinecraftServer): ScopeID =
        System.identityHashCode(server).toString()

    fun getScope(server: MinecraftServer) : Scope? {
        return try {
            koin.getScope(getScopeId(server))
        } catch (e: ScopeNotCreatedException) {
            null
        }
    }

    init {
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            val packExists = server.lobbyWorld != null
            if (!packExists) {
                log.error("$MOD_ID datapack not found, exiting...")
                return@register
            }

            // obtain the bingo game state from the save file
            val state: BingoState = persistentStateFactory.fromWorld(server.overworld)

            // if the game was previously running, increment its updated timestamp by the time spent offline
            // (this is so that the timer only counts the amount of time spent in-game or with the server running)
            state.updatedAt?.let { lastUpdate ->
                state.timeOffline += (Clock.System.now() - lastUpdate)
            }

            log.info("[ScopeManager] Starting server scope...")
            val ctx = BingoContext(server, state)
            val scopeId = getScopeId(server)
            val scope = koin.createScope<BingoContext>(scopeId, ctx)
            scope.commonInit()

            // re-run state entry listeners
            scope.get<ScopedEvents>().onStateChange(state.state to state.state)
        }

        ServerLifecycleEvents.SERVER_STOPPED.register { server ->
            val scope = getScope(server) ?: return@register
            log.info("[ScopeManager] Closing server scope...")
            onCleanup(scope)
            koin.deleteScope(getScopeId(server))
        }
    }

    companion object {
        val onCleanup = EventListener<Scope>()
    }

}