package me.jfenn.bingo.client.common.hud

import me.jfenn.bingo.client.common.state.BingoHudState
import me.jfenn.bingo.client.platform.IClient
import me.jfenn.bingo.client.platform.renderer.IDrawService
import me.jfenn.bingo.client.platform.renderer.use
import me.jfenn.bingo.common.menu.tooltips.TooltipPacket
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.utils.div
import me.jfenn.bingo.common.utils.milliseconds
import me.jfenn.bingo.common.utils.minus
import me.jfenn.bingo.platform.EntityHitResult
import java.time.Instant
import java.util.*

internal class TooltipHudRenderer(
    private val state: BingoHudState,
    private val client: IClient
) : BingoComponent() {

    private var lookingAtEntity: UUID? = null
    private var lookingAtTicks: Int = 0
    private var tooltipPacket: TooltipPacket? = null
    private var tooltipStartedAt: Instant = Instant.MIN
    private var tooltipUpdatedAt: Instant = Instant.MIN

    fun tick() {
        val hitResult = client.lookingAt() as? EntityHitResult
        if (lookingAtEntity == hitResult?.entityId) {
            lookingAtTicks = (lookingAtTicks + 1).coerceAtMost(20)
        } else {
            lookingAtEntity = hitResult?.entityId
            lookingAtTicks = 0
        }

        val tooltip = state.tooltipsByEntity[lookingAtEntity ?: return] ?: return
        if (lookingAtTicks >= 20) {
            if (tooltipPacket == null) {
                tooltipStartedAt = state.now
            }

            tooltipPacket = tooltip
            tooltipUpdatedAt = state.now
        }
    }

    fun drawTooltip(drawService: IDrawService) {
        val tooltipPacket = tooltipPacket ?: return

        val now = state.now
        val tooltipText = tooltipPacket.text.let { tooltipText ->
            // Get or create clientWrappedText
            tooltipPacket.clientWrappedText
                ?: tooltipText.flatMap { line -> drawService.font.wrapLines(line, 200) }
                    .also { tooltipPacket.clientWrappedText = it }
        }

        val tooltipSize = tooltipText.maxOfOrNull { drawService.font.getTextWidth(it) } ?: 0
        val tooltipX = drawService.window.scaledWindowWidth/2 - tooltipSize/2 - 12
        val tooltipY = (drawService.window.scaledWindowHeight * 0.5).toInt() + 18 + 12

        val fadeIn = ((now - tooltipStartedAt) / 250.milliseconds).coerceIn(0.0, 1.0)
        val fadeOut = 1.0 - ((now - tooltipUpdatedAt.plus(100.milliseconds)) / 250.milliseconds).coerceIn(0.0, 1.0)

        drawService.matrices.use {
            drawService.matrices.translate(0f, 0f, 2000f)
            drawService.setShaderColor(1f, 1f, 1f, (fadeIn * fadeOut).toFloat())
            drawService.drawTooltip(tooltipText, tooltipX, tooltipY)
            drawService.drawTooltipImmediate()
            drawService.clearShaderColor()
        }

        if (fadeOut <= 0.0) {
            this.tooltipPacket = null
        }
    }
}