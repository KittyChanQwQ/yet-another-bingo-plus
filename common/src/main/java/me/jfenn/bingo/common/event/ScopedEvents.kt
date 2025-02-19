package me.jfenn.bingo.common.event

import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.utils.EventListenerFactory
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.event.IEvent
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.model.PlayerEvent
import me.jfenn.bingo.platform.event.model.TickEvent
import me.jfenn.bingo.platform.packet.IServerPacketHandlerC2S
import me.jfenn.bingo.platform.packet.ServerPacket
import me.jfenn.bingo.platform.utils.IEventListener
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeCallback
import org.slf4j.Logger
import java.util.*

class ScopedEvents(
    private val log: Logger,
    private val eventBus: IEventBus,
) : BingoComponent(), ScopeCallback {

    private object UpdateTick: IEvent<TickEvent>

    init {
        eventBus.register(TickEvent.Start) {
            if (it.ticks % 10 == 0) {
                eventBus.emit(UpdateTick, it)
            }
        }
    }

    fun onGameTick(callback: (TickEvent) -> Unit) {
        eventBus.register(TickEvent.Start, callback)
    }

    fun onUpdateTick(callback: (TickEvent) -> Unit) {
        eventBus.register(UpdateTick, callback)
    }

    /**
     * Note: this event is NOT called on the server thread!
     */
    fun onPlayerInit(callback: (PlayerEvent) -> Unit) = eventBus.register(PlayerEvent.Init, callback)
    fun onPlayerJoin(callback: (PlayerEvent) -> Unit) = eventBus.register(PlayerEvent.Join, callback)
    fun onPlayerDisconnect(callback: (PlayerEvent) -> Unit) = eventBus.register(PlayerEvent.Disconnect, callback)
    fun onPlayerRespawn(callback: (PlayerEvent) -> Unit) = eventBus.register(PlayerEvent.AfterRespawn, callback)
    fun onPlayerChannelRegister(callback: (PlayerEvent) -> Unit) = eventBus.register(PlayerEvent.ChannelRegister, callback)

    val onStateChange: IEventListener<Pair<GameState, GameState>> by EventListenerFactory()

    val onChangeOptions: IEventListener<Unit> by EventListenerFactory()
    val onChangePlayerSettings: IEventListener<IPlayerHandle> by EventListenerFactory()
    val onChangeTeam: IEventListener<IPlayerHandle> by EventListenerFactory()

    val onCardShuffled: IEventListener<UUID> by EventListenerFactory()
    val onTeamWinner: IEventListener<BingoTeam> by EventListenerFactory()

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
        eventBus.register(packet, callback)
    }

    override fun onScopeClose(scope: Scope) {
        onClose.invoke(Unit)
    }
}