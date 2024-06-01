package me.jfenn.bingo.client.impl

import me.jfenn.bingo.api.ICloseEvent
import me.jfenn.bingo.common.utils.EventListener
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents

object ClientCloseEvent : ICloseEvent {
    override val onClose = EventListener<Unit>()

    init {
        ClientLifecycleEvents.CLIENT_STOPPING.register {
            onClose(Unit)
        }
    }
}