package me.jfenn.bingo.impl.event

import me.jfenn.bingo.common.utils.EventListenerFactory
import me.jfenn.bingo.platform.ICloseEvent
import me.jfenn.bingo.platform.utils.IEventListener
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents

object ServerCloseEvent : ICloseEvent {
    override val onClose: IEventListener<Unit> by EventListenerFactory()

    init {
        ServerLifecycleEvents.SERVER_STOPPING.register {
            onClose(Unit)
        }
    }
}
