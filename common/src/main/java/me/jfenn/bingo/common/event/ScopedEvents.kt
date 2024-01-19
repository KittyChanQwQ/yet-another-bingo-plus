package me.jfenn.bingo.common.event

import me.jfenn.bingo.common.event.model.UseBlockEvent
import me.jfenn.bingo.common.event.model.UseEntityEvent
import me.jfenn.bingo.common.event.model.UseItemEvent
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.utils.EventListener
import me.jfenn.bingo.common.utils.ReturnEventListener
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.ActionResult
import net.minecraft.util.TypedActionResult

class ScopedEvents {
    val onGameTick = EventListener<Unit>()
    val onUpdateTick = EventListener<Unit>()

    val onPlayerJoin = EventListener<ServerPlayerEntity>()
    val onPlayerLeave = EventListener<ServerPlayerEntity>()
    val onPlayerRespawn = EventListener<ServerPlayerEntity>()

    val onEntityLoad = EventListener<Pair<Entity, ServerWorld>>()
    val onUseBlock = ReturnEventListener<UseBlockEvent, ActionResult>()
    val onUseEntity = ReturnEventListener<UseEntityEvent, ActionResult>()
    val onUseItem = ReturnEventListener<UseItemEvent, TypedActionResult<ItemStack>>()

    val onStateChange = EventListener<Pair<GameState, GameState>>()

    val onChangeConfig = EventListener<Unit>()
    val onChangePlayerSettings = EventListener<ServerPlayerEntity>()

    fun onEnter(
        state: GameState,
        requireChange: Boolean = false,
        callback: (GameState) -> Unit,
    ) {
        onStateChange { (from, to) ->
            if (to == state && (!requireChange || from != to)) {
                callback(from)
            }
        }
    }
}