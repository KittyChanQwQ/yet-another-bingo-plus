package me.jfenn.bingo.common.event

import net.minecraft.entity.decoration.InteractionEntity
import net.minecraft.server.network.ServerPlayerEntity
import java.util.*

object InteractionEntityEvents {

    val INTERACT_LISTENERS = HashMap<UUID, (ServerPlayerEntity) -> Unit>()

    fun onInteract(entity: InteractionEntity, consumer: (ServerPlayerEntity) -> Unit) {
        INTERACT_LISTENERS[entity.uuid] = consumer
    }

}