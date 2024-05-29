package me.jfenn.bingo.common.scope

import me.jfenn.bingo.api.IPersistentStateManager
import me.jfenn.bingo.common.MOD_ID
import me.jfenn.bingo.common.commonInit
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.lobbyWorld
import me.jfenn.bingo.common.state.BingoState
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
    private val persistentStateManager: IPersistentStateManager,
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
            val state: BingoState = persistentStateManager.getFromWorld(
                world = server.overworld,
                id = MOD_ID,
                clazz = BingoState::class,
                default = { BingoState() },
            )

            log.info("[ScopeManager] Starting server scope...")
            val ctx = BingoScope(server, state)
            val scopeId = getScopeId(server)
            val scope = koin.createScope<BingoScope>(scopeId, ctx)
            scope.commonInit()

            val events = scope.get<ScopedEvents>()
            scope.registerCallback(events)

            // re-run state entry listeners
            events.onStateChange(state.state to state.state)
        }

        ServerLifecycleEvents.SERVER_STOPPED.register { server ->
            val scope = getScope(server) ?: return@register
            log.info("[ScopeManager] Closing server scope...")
            onCleanup(scope)
            scope.close()
        }
    }

    companion object {
        val onCleanup = EventListener<Scope>()
    }

}