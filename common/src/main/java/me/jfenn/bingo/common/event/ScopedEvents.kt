package me.jfenn.bingo.common.event

import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.utils.EventListenerFactory
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.event.model.*
import me.jfenn.bingo.platform.item.IItemStack
import me.jfenn.bingo.platform.packet.IServerPacketHandlerC2S
import me.jfenn.bingo.platform.packet.ServerPacket
import me.jfenn.bingo.platform.utils.IEventListener
import me.jfenn.bingo.platform.utils.IReturnEventListener
import net.minecraft.entity.Entity
import net.minecraft.server.world.ServerWorld
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeCallback
import org.slf4j.Logger

class ScopedEvents(
    private val log: Logger,
) : BingoComponent(), ScopeCallback {

    val onGameTick: IEventListener<Unit> by EventListenerFactory()
    val onGameTickEnd: IEventListener<Unit> by EventListenerFactory()
    val onUpdateTick: IEventListener<Unit> by EventListenerFactory()

    /**
     * Note: this event is NOT called on the server thread!
     */
    val onPlayerInit: IEventListener<IPlayerHandle> by EventListenerFactory()
    val onPlayerJoin: IEventListener<IPlayerHandle> by EventListenerFactory()
    val onPlayerDisconnect: IEventListener<IPlayerHandle> by EventListenerFactory()
    val onPlayerRespawn: IEventListener<PlayerEvent> by EventListenerFactory()
    val onPlayerChannelRegister: IEventListener<IPlayerHandle> by EventListenerFactory()

    val onEntityLoad: IEventListener<Pair<Entity, ServerWorld>> by EventListenerFactory()
    val onUseBlock: IReturnEventListener<UseBlockEvent, ActionResult<Unit>?> by EventListenerFactory()
    val onUseEntity: IReturnEventListener<UseEntityEvent, ActionResult<Unit>?> by EventListenerFactory()
    val onUseItem: IReturnEventListener<UseItemEvent, ActionResult<IItemStack?>?> by EventListenerFactory()
    val onAttackEntity: IReturnEventListener<AttackEntityEvent, ActionResult<Unit>?> by EventListenerFactory()
    val onAttackBlock: IReturnEventListener<AttackBlockEvent, ActionResult<Unit>?> by EventListenerFactory()

    val onStateChange: IEventListener<Pair<GameState, GameState>> by EventListenerFactory()

    val onChangeOptions: IEventListener<Unit> by EventListenerFactory()
    val onChangePlayerSettings: IEventListener<IPlayerHandle> by EventListenerFactory()
    val onChangeTeam: IEventListener<IPlayerHandle> by EventListenerFactory()

    val onTeamWinner: IEventListener<BingoTeam> by EventListenerFactory()

    val onDataReload: IEventListener<ReloadEvent> by EventListenerFactory()

    val onClose: IEventListener<Unit> by EventListenerFactory()

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