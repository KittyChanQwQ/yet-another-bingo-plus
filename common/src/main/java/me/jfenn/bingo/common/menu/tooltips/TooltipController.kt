package me.jfenn.bingo.common.menu.tooltips

import me.jfenn.bingo.common.event.model.StateChangedEvent
import me.jfenn.bingo.common.event.packet.ServerPacketEvents
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.platform.IPlayerManager
import me.jfenn.bingo.platform.event.ICallbackHandle
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.model.PlayerEvent
import me.jfenn.bingo.platform.event.model.TickEvent
import java.util.*

internal class TooltipController(
    private val state: BingoState,
    private val tooltipState: TooltipState,
    private val packets: ServerPacketEvents,
    private val playerManager: IPlayerManager,
    private val eventBus: IEventBus,
) {

    private val tooltipsByPlayer = mutableMapOf<UUID, MutableSet<UUID>>()

    fun tick() {
        val tooltips = tooltipState.tooltips()
        for (player in playerManager.getPlayers()) {
            val sentTooltips = tooltipsByPlayer.getOrPut(player.uuid) { mutableSetOf() }
            for (tooltip in tooltips) {
                if (sentTooltips.add(tooltip.entityId)) {
                    packets.tooltipV1.send(player, tooltip)
                }
            }
        }
    }

    var callback: ICallbackHandle? = null

    init {
        eventBus.register(StateChangedEvent) {
            callback?.close()
            callback = if (state.isLobbyMode && it.to == GameState.PREGAME) {
                eventBus.register(TickEvent.Start) { tick() }
            } else {
                null
            }
        }

        eventBus.register(PlayerEvent.ChannelRegister) {
            tooltipsByPlayer.remove(it.player.uuid)
        }
    }
}
