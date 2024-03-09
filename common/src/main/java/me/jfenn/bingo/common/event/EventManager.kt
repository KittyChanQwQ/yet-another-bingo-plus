package me.jfenn.bingo.common.event

import me.jfenn.bingo.common.event.model.*
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.scope.ScopeManager
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.*
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
    private val scopeManager: ScopeManager,
) : BingoComponent() {

    private fun getEvents(server: MinecraftServer?) : ScopedEvents? {
        server ?: return null
        val scope = scopeManager.getScope(server) ?: return null
        return scope.get<ScopedEvents>()
    }

    init {
        ServerTickEvents.START_SERVER_TICK.register { server ->
            val events = getEvents(server) ?: return@register
            events.onGameTick(Unit)

            if (server.ticks % 10 == 0) {
                events.onUpdateTick(Unit)
            }
        }

        ServerPlayConnectionEvents.JOIN.register { handler, _, server ->
            server.execute {
                val events = getEvents(server) ?:return@execute
                events.onPlayerJoin.invoke(handler.player)

                // onChangePlayerSettings should also be invoked if a player logs in
                events.onChangePlayerSettings.invoke(handler.player)
            }
        }

        ServerPlayConnectionEvents.DISCONNECT.register { handler, server ->
            server.execute {
                getEvents(server)?.onPlayerLeave?.invoke(handler.player)
            }
        }

        ServerPlayerEvents.AFTER_RESPAWN.register { _, player, _ ->
            getEvents(player.server)?.onPlayerRespawn?.invoke(player)
        }

        ServerEntityEvents.ENTITY_LOAD.register { entity, world ->
            getEvents(world.server)?.onEntityLoad?.invoke(entity to world)
        }

        UseBlockCallback.EVENT.register { player, world, hand, hit ->
            val results = getEvents(world.server)?.onUseBlock?.invoke(
                UseBlockEvent(player, world, hand, hit)
            ) ?: emptyList()

            when {
                ActionResult.FAIL in results -> ActionResult.FAIL
                ActionResult.SUCCESS in results -> ActionResult.SUCCESS
                else -> ActionResult.PASS
            }
        }

        UseEntityCallback.EVENT.register { player, world, hand, entity, hit ->
            val results = getEvents(world.server)?.onUseEntity?.invoke(
                UseEntityEvent(player, world, hand, entity, hit)
            ) ?: emptyList()

            when {
                ActionResult.FAIL in results -> ActionResult.FAIL
                ActionResult.SUCCESS in results -> ActionResult.SUCCESS
                else -> ActionResult.PASS
            }
        }

        UseItemCallback.EVENT.register { player, world, hand ->
            val results = getEvents(world.server)?.onUseItem?.invoke(
                UseItemEvent(player, world, hand)
            ) ?: emptyList()

            results.find { it?.result == ActionResult.FAIL }
                ?.let { return@register it }

            results.find { it?.result == ActionResult.SUCCESS }
                ?.let { return@register it }

            // Otherwise, return a PASS result (preferring a returned value)
            results.find { it?.result == ActionResult.PASS }
                ?: TypedActionResult.pass(null)
        }

        AttackEntityCallback.EVENT.register { player, world, hand, entity, hit ->
            val results = getEvents(world.server)?.onAttackEntity?.invoke(
                AttackEntityEvent(player, world, hand, entity, hit)
            ) ?: emptyList()

            when {
                ActionResult.FAIL in results -> ActionResult.FAIL
                ActionResult.SUCCESS in results -> ActionResult.SUCCESS
                else -> ActionResult.PASS
            }
        }

        AttackBlockCallback.EVENT.register { player, world, hand, blockPos, direction ->
            val results = getEvents(world.server)?.onAttackBlock?.invoke(
                AttackBlockEvent(player, world, hand, blockPos, direction)
            ) ?: emptyList()

            when {
                ActionResult.FAIL in results -> ActionResult.FAIL
                ActionResult.SUCCESS in results -> ActionResult.SUCCESS
                else -> ActionResult.PASS
            }
        }
    }

}