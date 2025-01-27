package me.jfenn.bingo.common.event

import me.jfenn.bingo.platform.IEntity
import me.jfenn.bingo.platform.scope.BingoKoin
import me.jfenn.bingo.common.scope.ScopeManager
import net.minecraft.entity.decoration.InteractionEntity
import net.minecraft.server.network.ServerPlayerEntity
import org.slf4j.Logger
import java.util.*

class InteractionEntityEvents(
    private val log: Logger,
) {

    val INTERACT_LISTENERS = HashMap<UUID, (ServerPlayerEntity) -> Unit>()

    fun onInteract(entity: IEntity, consumer: (ServerPlayerEntity) -> Unit) {
        INTERACT_LISTENERS[entity.uuid] = consumer
    }

    companion object {

        fun triggerInteract(entity: InteractionEntity, player: ServerPlayerEntity): Boolean {
            val server = player.server

            val scopeManager = BingoKoin.koinApp.koin.get<ScopeManager>()
            val scope = scopeManager.getScope(server) ?: return false

            val events = scope.get<InteractionEntityEvents>()
            val callback = events.INTERACT_LISTENERS[entity.uuid]
            try {
                callback?.invoke(player)
            } catch (e: Throwable) {
                events.log.error("Error running callback for interaction entity", e)
            }
            return callback != null
        }

    }

}