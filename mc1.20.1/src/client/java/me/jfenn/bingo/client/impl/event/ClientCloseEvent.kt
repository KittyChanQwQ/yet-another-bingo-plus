package me.jfenn.bingo.client.impl.event

import me.jfenn.bingo.common.utils.EventListenerFactory
import me.jfenn.bingo.platform.ICloseEvent
import me.jfenn.bingo.platform.utils.IEventListener
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents

internal object ClientCloseEvent : ICloseEvent {
    override val onClose: IEventListener<Unit> by EventListenerFactory()

    init {
        ClientLifecycleEvents.CLIENT_STOPPING.register {
            onClose(Unit)
        }
    }
}