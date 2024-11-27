package me.jfenn.bingo.impl

import me.jfenn.bingo.platform.ICloseEvent
import me.jfenn.bingo.common.utils.EventListener
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents

object ServerCloseEvent : ICloseEvent {
    override val onClose = EventListener<Unit>()

    init {
        ServerLifecycleEvents.SERVER_STOPPING.register {
            onClose(Unit)
        }
    }
}
