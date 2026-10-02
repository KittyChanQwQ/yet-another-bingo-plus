package me.jfenn.bingo.client.common.hud.screen

import me.jfenn.bingo.client.platform.IClient
import me.jfenn.bingo.client.platform.renderer.IDrawService
import me.jfenn.bingo.client.platform.renderer.use
import me.jfenn.bingo.client.platform.screen.IDrawable
import me.jfenn.bingo.platform.Identifier
import me.jfenn.bingo.platform.text.IText
import org.joml.Vector2i
import org.koin.core.Koin

class GameInfoWidget(
    koin: Koin,
    private val client: IClient = koin.get(),
) : IDrawable {

    var text: List<IText> = emptyList()
        set(value) {
            field = value
            resize()
        }

    var position: Vector2i = Vector2i(0, 0)
    var size: Vector2i = Vector2i(0, 0)

    private var statusText: List<IText> = emptyList()
    private var statusTextWidths: List<Int> = emptyList()

    fun resize() {
        statusText = text
        statusTextWidths = statusText.map { client.font.getTextWidth(it) }
        size.y = (statusText.size * (client.font.getTextHeight() + 2)) + 8 + 8
    }

    private fun renderGameStatus(drawService: IDrawService) {
        var titleY = 9

        drawService.drawNinePatch(
            texture = Identifier.of("minecraft", "bingo/game_container"),
            x = 0,
            y = 0,
            width = size.x,
            height = size.y,
            sliceSize = 8,
            textureWidth = 200,
            textureHeight = 40,
        )

        for ((line, lineWidth) in statusText.zip(statusTextWidths)) {
            drawService.drawText(
                line,
                (size.x - lineWidth)/2,
                titleY,
                0xff_ffffff.toInt(),
                false,
            )
            titleY += drawService.font.getTextHeight() + 2
        }
    }

    override fun render(drawService: IDrawService) {
        drawService.matrices.use {
            drawService.matrices.translate(position.x.toFloat(), position.y.toFloat(), 0f)
            renderGameStatus(drawService)
        }
    }
}