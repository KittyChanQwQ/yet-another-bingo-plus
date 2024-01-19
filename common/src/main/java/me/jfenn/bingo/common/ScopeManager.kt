package me.jfenn.bingo.common

import kotlinx.datetime.Clock
import me.jfenn.bingo.common.datapack.DatapackService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.state.*
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
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
            log.error("Error fetching scope for server", e)
            null
        }
    }

    init {
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            val packExists = server.worlds.any { it.registryKey.value == LOBBY_WORLD_ID }
            if (!packExists) {
                log.error("$MOD_ID datapack not found, exiting...")
            }

            // obtain the bingo game state from the save file
            val state: BingoState = persistentStateFactory.fromWorld(server.overworld)

            // if the game was previously running, increment its updated timestamp by the time spent offline
            // (this is so that the timer only counts the amount of time spent in-game or with the server running)
            state.updatedAt?.let { lastUpdate ->
                state.timeOffline += (Clock.System.now() - lastUpdate)
            }

            val ctx = BingoContext(server, state)
            val scopeId = getScopeId(server)
            val scope = koin.createScope<BingoContext>(scopeId, ctx)
            scope.commonInit()

            // re-run state entry listeners
            scope.get<ScopedEvents>().onStateChange(state.state to state.state)
        }

        ServerLifecycleEvents.SERVER_STOPPED.register { server ->
            koin.deleteScope(getScopeId(server))
        }
    }

}