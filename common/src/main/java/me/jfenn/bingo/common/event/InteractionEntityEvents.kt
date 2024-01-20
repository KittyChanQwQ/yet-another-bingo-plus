package me.jfenn.bingo.common.event

import me.jfenn.bingo.common.scope.BingoKoin
import me.jfenn.bingo.common.scope.ScopeManager
import me.jfenn.bingo.common.utils.EventListener
import net.minecraft.entity.decoration.InteractionEntity
import net.minecraft.server.network.ServerPlayerEntity
import java.util.*

class InteractionEntityEvents {

    val INTERACT_LISTENERS = HashMap<UUID, (ServerPlayerEntity) -> Unit>()

    val onInteract = EventListener<Pair<InteractionEntity, ServerPlayerEntity>>()

    fun onInteract(entity: InteractionEntity, consumer: (ServerPlayerEntity) -> Unit) {
        INTERACT_LISTENERS[entity.uuid] = consumer
    }

    companion object {

        fun triggerInteract(entity: InteractionEntity, player: ServerPlayerEntity): Boolean {
            val server = player.server

            val scopeManager = BingoKoin.koinApp.koin.get<ScopeManager>()
            val scope = scopeManager.getScope(server) ?: return false

            val events = scope.get<InteractionEntityEvents>()
            val callback = events.INTERACT_LISTENERS[entity.uuid]
            callback?.invoke(player)
            events.onInteract(entity to player)
            return callback != null
        }

    }

}