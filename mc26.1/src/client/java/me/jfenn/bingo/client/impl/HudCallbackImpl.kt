package me.jfenn.bingo.client.impl

import me.jfenn.bingo.client.impl.draw.DrawService
import me.jfenn.bingo.client.platform.event.model.HudRenderEvent
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.config.CardOverlap
import me.jfenn.bingo.platform.event.IEventBus
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier

class HudCallbackImpl(
    private val eventBus: IEventBus,
    private val config: BingoConfig,
) {
    private fun emitRenderEvent(drawContext: GuiGraphicsExtractor, renderTickCounter: DeltaTracker) {
        val delta = renderTickCounter.getGameTimeDeltaPartialTick(false)
        eventBus.emit(
            HudRenderEvent,
            HudRenderEvent(
                drawService = DrawService(drawContext)
                    .also { it.delta = delta },
                delta = renderTickCounter.getGameTimeDeltaPartialTick(false)
            )
        )
    }

    init {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID_BINGO, "card_above")) { drawContext, renderTickCounter ->
            if (config.client.cardOverlap == CardOverlap.ABOVE) {
                emitRenderEvent(drawContext, renderTickCounter)
            }
        }

        HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR, Identifier.fromNamespaceAndPath(MOD_ID_BINGO, "card_underneath")) { drawContext, renderTickCounter ->
            if (config.client.cardOverlap == CardOverlap.UNDERNEATH) {
                emitRenderEvent(drawContext, renderTickCounter)
            }
        }
    }
}