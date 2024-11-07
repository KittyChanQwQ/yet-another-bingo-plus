package me.jfenn.bingo.common.event

import me.jfenn.bingo.api.IServerEvents
import me.jfenn.bingo.common.event.model.ActionResult
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.scope.ScopeManager
import net.minecraft.server.MinecraftServer

/**
 * Binds the Fabric API events to a scoped event system in [ScopedEvents]
 *
 * Must be initialized only once!
 */
class EventManager(
    private val scopeManager: ScopeManager,
    serverEvents: IServerEvents
) : BingoComponent() {

    private fun getEvents(server: MinecraftServer?) : ScopedEvents? {
        server ?: return null
        val scope = scopeManager.getScope(server) ?: return null
        return scope.get<ScopedEvents>()
    }

    init {
        serverEvents.onTickStart { server ->
            val events = getEvents(server) ?: return@onTickStart
            events.onGameTick(Unit)

            if (server.ticks % 10 == 0) {
                events.onUpdateTick(Unit)
            }
        }

        serverEvents.onTickEnd { server ->
            getEvents(server)
                ?.onGameTickEnd
                ?.invoke(Unit)
        }

        serverEvents.onPlayerJoin { event ->
            event.server.execute {
                getEvents(event.server)
                    ?.onPlayerJoin
                    ?.invoke(event.player)
            }
        }

        serverEvents.onPlayerDisconnect { event ->
            event.server.execute {
                getEvents(event.server)
                    ?.onPlayerLeave
                    ?.invoke(event.player)
            }
        }

        serverEvents.onAfterRespawn { event ->
            getEvents(event.server)?.onPlayerRespawn?.invoke(event)
        }

        serverEvents.onEntityLoad { event ->
            getEvents(event.server)?.onEntityLoad?.invoke(event.entity to event.world)
        }

        serverEvents.onUseBlock { event ->
            val results = getEvents(event.server)?.onUseBlock?.invoke(event)
                ?: emptyList()

            ActionResult.collapse(results) ?: ActionResult.Pass(Unit)
        }

        serverEvents.onUseEntity { event ->
            val results = getEvents(event.server)?.onUseEntity?.invoke(event)
                ?: emptyList()

            ActionResult.collapse(results) ?: ActionResult.Pass(Unit)
        }

        serverEvents.onUseItem { event ->
            val results = getEvents(event.server)?.onUseItem?.invoke(event)
                ?: emptyList()

            ActionResult.collapse(results) ?: ActionResult.Pass(null)
        }

        serverEvents.onAttackEntity { event ->
            val results = getEvents(event.server)?.onAttackEntity?.invoke(event)
                ?: emptyList()

            ActionResult.collapse(results) ?: ActionResult.Pass(Unit)
        }

        serverEvents.onAttackBlock { event ->
            val results = getEvents(event.server)?.onAttackBlock?.invoke(event)
                ?: emptyList()

            ActionResult.collapse(results) ?: ActionResult.Pass(Unit)
        }

        serverEvents.onPlayerChannelRegister { event ->
            event.server.execute {
                getEvents(event.server)?.onPlayerChannelRegister?.invoke(event.player)
            }
        }

        serverEvents.onDataReload { event ->
            getEvents(event.server)?.onDataReload?.invoke(event)
        }
    }

}