package me.jfenn.bingo.client.common.hud

import me.jfenn.bingo.client.platform.renderer.IDrawService
import me.jfenn.bingo.client.platform.renderer.IDrawServiceFactory
import me.jfenn.bingo.client.platform.renderer.IFramebuffer
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.team.BingoTeamKey
import kotlin.math.roundToInt

internal class CardBufferService(
    private val state: BingoHudState,
    private val config: BingoConfig,
    private val hudRenderer: BingoHudRenderer,
    private val drawServiceFactory: IDrawServiceFactory,
) {

    private var framebuffer: IFramebuffer? = null

    fun resize() {
        val width = (BingoHudRenderer.CARD_WIDTH * config.client.cardScale).roundToInt()
        val height = (BingoHudRenderer.CARD_HEIGHT * config.client.cardScale).roundToInt()
        framebuffer?.resize(width, height)
    }

    fun update(teamKey: BingoTeamKey?) {
        val framebuffer = framebuffer ?: run {
            val f = drawServiceFactory.newBuffer(1, 1)
            framebuffer = f
            resize()
            f
        }

        val view = state.views[teamKey] ?: return
        val drawService = framebuffer.beginWrite()

        drawService.matrices.push()
        drawService.matrices.scale(config.client.cardScale, config.client.cardScale, 1f)
        hudRenderer.drawCard(drawService, view)
        drawService.matrices.pop()

        framebuffer.endWrite()
    }

    fun draw(service: IDrawService, teamKey: BingoTeamKey?, width: Int, height: Int) {
        framebuffer?.draw(service, width, height)
    }

}