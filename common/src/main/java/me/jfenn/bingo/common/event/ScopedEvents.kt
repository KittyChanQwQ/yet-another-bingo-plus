package me.jfenn.bingo.common.event

import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.utils.EventListener
import me.jfenn.bingo.common.utils.ReturnEventListener
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.event.model.*
import me.jfenn.bingo.platform.item.IItemStack
import me.jfenn.bingo.platform.packet.IServerPacketHandlerC2S
import me.jfenn.bingo.platform.packet.ServerPacket
import net.minecraft.entity.Entity
import net.minecraft.server.world.ServerWorld
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeCallback
import org.slf4j.Logger

class ScopedEvents(
    private val log: Logger,
) : BingoComponent(), ScopeCallback {

    val onGameTick = EventListener<Unit>()
    val onGameTickEnd = EventListener<Unit>()
    val onUpdateTick = EventListener<Unit>()

    /**
     * Note: this event is NOT called on the server thread!
     */
    val onPlayerInit = EventListener<IPlayerHandle>()
    val onPlayerJoin = EventListener<IPlayerHandle>()
    val onPlayerDisconnect = EventListener<IPlayerHandle>()
    val onPlayerRespawn = EventListener<PlayerEvent>()
    val onPlayerChannelRegister = EventListener<IPlayerHandle>()

    val onEntityLoad = EventListener<Pair<Entity, ServerWorld>>()
    val onUseBlock = ReturnEventListener<UseBlockEvent, ActionResult<Unit>?>()
    val onUseEntity = ReturnEventListener<UseEntityEvent, ActionResult<Unit>?>()
    val onUseItem = ReturnEventListener<UseItemEvent, ActionResult<IItemStack?>?>()
    val onAttackEntity = ReturnEventListener<AttackEntityEvent, ActionResult<Unit>?>()
    val onAttackBlock = ReturnEventListener<AttackBlockEvent, ActionResult<Unit>?>()

    val onStateChange = EventListener<Pair<GameState, GameState>>()

    val onChangeOptions = EventListener<Unit>()
    val onChangePlayerSettings = EventListener<IPlayerHandle>()
    val onChangeTeam = EventListener<IPlayerHandle>()

    val onTeamWinner = EventListener<BingoTeam>()

    val onDataReload = EventListener<ReloadEvent>()

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
        val handle = packet.onPacket(callback)
        onClose { packet.onPacket.unregister(handle) }
    }

    override fun onScopeClose(scope: Scope) {
        onClose.invoke(Unit)
    }
}