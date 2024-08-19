package me.jfenn.bingo.common.event

import me.jfenn.bingo.api.IServerPacketHandlerC2S
import me.jfenn.bingo.api.ServerPacket
import me.jfenn.bingo.common.event.model.*
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.utils.EventListener
import me.jfenn.bingo.common.utils.ReturnEventListener
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.ActionResult
import net.minecraft.util.TypedActionResult
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeCallback
import org.slf4j.Logger

class ScopedEvents(
    private val log: Logger,
) : BingoComponent(), ScopeCallback {

    val onGameTick = EventListener<Unit>()
    val onGameTickEnd = EventListener<Unit>()
    val onUpdateTick = EventListener<Unit>()

    val onPlayerJoin = EventListener<ServerPlayerEntity>()
    val onPlayerLeave = EventListener<ServerPlayerEntity>()
    val onPlayerRespawn = EventListener<ServerPlayerEntity>()
    val onPlayerChannelRegister = EventListener<ServerPlayerEntity>()

    val onEntityLoad = EventListener<Pair<Entity, ServerWorld>>()
    val onUseBlock = ReturnEventListener<UseBlockEvent, ActionResult?>()
    val onUseEntity = ReturnEventListener<UseEntityEvent, ActionResult?>()
    val onUseItem = ReturnEventListener<UseItemEvent, TypedActionResult<ItemStack>?>()
    val onAttackEntity = ReturnEventListener<AttackEntityEvent, ActionResult?>()
    val onAttackBlock = ReturnEventListener<AttackBlockEvent, ActionResult?>()

    val onStateChange = EventListener<Pair<GameState, GameState>>()

    val onChangeOptions = EventListener<Unit>()
    val onChangePlayerSettings = EventListener<ServerPlayerEntity>()

    val onClose = EventListener<Unit>()

    init {
        onStateChange { (from, to) ->
            log.info("GameState changed: {} -> {}", from, to)
        }
    }

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

    fun <T> onPacket(packet: IServerPacketHandlerC2S<T>, callback: (ServerPacket<T>) -> Unit) {
        packet.onPacket(callback)
        onClose { packet.onPacket.unregister(callback) }
    }

    override fun onScopeClose(scope: Scope) {
        onClose.invoke(Unit)
    }
}