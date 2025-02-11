package me.jfenn.bingo.client.impl

import me.jfenn.bingo.client.platform.renderer.IDrawServiceFactory
import me.jfenn.bingo.client.platform.IHudCallback
import me.jfenn.bingo.common.utils.EventListener
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback

class HudCallbackImpl(
    private val drawServiceFactory: IDrawServiceFactory,
) : IHudCallback {
    override val onRenderCard = EventListener<IHudCallback.RenderEvent>()

    init {
        HudRenderCallback.EVENT.register { drawContext, delta ->
            onRenderCard.invoke(
                IHudCallback.RenderEvent(
                    drawService = drawServiceFactory.create(drawContext),
                    delta = delta
                )
            )
        }
    }
}