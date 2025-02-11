package me.jfenn.bingo.client.impl

import me.jfenn.bingo.client.platform.IHudCallback
import me.jfenn.bingo.client.platform.renderer.IDrawServiceFactory
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.utils.EventListener
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer
import net.minecraft.util.Identifier

class HudCallbackImpl(
    private val drawServiceFactory: IDrawServiceFactory,
) : IHudCallback {
    override val onRenderCard = EventListener<IHudCallback.RenderEvent>()

    init {
        HudLayerRegistrationCallback.EVENT.register { layeredDrawer ->
            layeredDrawer.attachLayerAfter(
                IdentifiedLayer.SCOREBOARD,
                Identifier.of(MOD_ID_BINGO, "card"),
                { drawContext, renderTickCounter ->
                    onRenderCard.invoke(
                        IHudCallback.RenderEvent(
                            drawService = drawServiceFactory.create(drawContext),
                            delta = renderTickCounter.getTickProgress(false)
                        )
                    )
                }
            )
        }
    }
}