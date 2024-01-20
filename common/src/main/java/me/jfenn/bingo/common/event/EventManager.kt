package me.jfenn.bingo.common.event

import me.jfenn.bingo.common.scope.ScopeManager
import me.jfenn.bingo.common.event.model.UseBlockEvent
import me.jfenn.bingo.common.event.model.UseEntityEvent
import me.jfenn.bingo.common.event.model.UseItemEvent
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.util.ActionResult
import net.minecraft.util.TypedActionResult

/**
 * Binds the Fabric API events to a scoped event system in [ScopedEvents]
 *
 * Must be initialized only once!
 */
class EventManager(
    val scopeManager: ScopeManager,
) {

    private fun getEvents(server: MinecraftServer?) : ScopedEvents? {
        server ?: return null
        val scope = scopeManager.getScope(server) ?: return null
        return scope.get<ScopedEvents>()
    }

    init {
        ServerTickEvents.START_SERVER_TICK.register { server ->
            val events = getEvents(server) ?: return@register
            events.onGameTick(Unit)

            if (server.ticks % 20 == 0) {
                events.onUpdateTick(Unit)
            }
        }

        ServerPlayConnectionEvents.JOIN.register { handler, _, server ->
            val events = getEvents(server) ?:return@register
            events.onPlayerJoin.invoke(handler.player)

            // onChangePlayerSettings should also be invoked if a player logs in
            events.onChangePlayerSettings.invoke(handler.player)
        }

        ServerPlayConnectionEvents.DISCONNECT.register { handler, server ->
            getEvents(server)?.onPlayerLeave?.invoke(handler.player)
        }

        ServerPlayerEvents.AFTER_RESPAWN.register { _, player, _ ->
            getEvents(player.server)?.onPlayerRespawn?.invoke(player)
        }

        ServerEntityEvents.ENTITY_LOAD.register { entity, world ->
            getEvents(world.server)?.onEntityLoad?.invoke(entity to world)
        }

        UseBlockCallback.EVENT.register { player, world, hand, hit ->
            getEvents(world.server)?.onUseBlock?.invoke(
                UseBlockEvent(player, world, hand, hit)
            )?.first() ?: ActionResult.PASS
        }

        UseEntityCallback.EVENT.register { player, world, hand, entity, hit ->
            getEvents(world.server)?.onUseEntity?.invoke(
                UseEntityEvent(player, world, hand, entity, hit)
            )?.first() ?: ActionResult.PASS
        }

        UseItemCallback.EVENT.register { player, world, hand ->
            getEvents(world.server)?.onUseItem?.invoke(
                UseItemEvent(player, world, hand)
            )?.first() ?: TypedActionResult.pass(null)
        }
    }

}