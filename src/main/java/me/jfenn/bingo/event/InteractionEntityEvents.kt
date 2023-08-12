package me.jfenn.bingo.event

import net.minecraft.entity.decoration.InteractionEntity
import net.minecraft.entity.player.PlayerEntity
import java.util.*

object InteractionEntityEvents {

    val INTERACT_LISTENERS = HashMap<UUID, (PlayerEntity) -> Unit>()

    fun onInteract(entity: InteractionEntity, consumer: (PlayerEntity) -> Unit) {
        INTERACT_LISTENERS[entity.uuid] = consumer
    }

}