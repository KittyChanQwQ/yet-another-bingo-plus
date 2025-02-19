package me.jfenn.bingo.client.impl

import me.jfenn.bingo.client.platform.event.model.HudRenderEvent
import me.jfenn.bingo.client.platform.renderer.IDrawServiceFactory
import me.jfenn.bingo.platform.event.IEventBus
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback

class HudCallbackImpl(
    private val drawServiceFactory: IDrawServiceFactory,
    private val eventBus: IEventBus,
) {
    init {
        HudRenderCallback.EVENT.register { drawContext, delta ->
            eventBus.emit(
                HudRenderEvent,
                HudRenderEvent(
                    drawService = drawServiceFactory.create(drawContext),
                    delta = delta
                )
            )
        }
    }
}