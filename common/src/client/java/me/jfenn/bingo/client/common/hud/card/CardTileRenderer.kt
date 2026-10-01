package me.jfenn.bingo.client.common.hud.card

import me.jfenn.bingo.client.common.DECORATION_ADVANCEMENT_ID
import me.jfenn.bingo.client.common.DECORATION_FORBIDDEN_ID
import me.jfenn.bingo.client.common.DECORATION_FREE_SPACE_ID
import me.jfenn.bingo.client.common.hud.card.ClientCardBufferRenderer.Companion.CARD_WIDTH
import me.jfenn.bingo.client.common.state.BingoHudState
import me.jfenn.bingo.client.platform.renderer.IDrawService
import me.jfenn.bingo.common.card.objective.display.Decoration
import me.jfenn.bingo.common.card.objective.display.ItemStyle
import me.jfenn.bingo.common.map.CardTile
import me.jfenn.bingo.common.map.CardTileImage
import me.jfenn.bingo.platform.Identifier

internal class CardTileRenderer(
    private val hudState: BingoHudState,
) {
    fun renderTile(
        drawService: IDrawService,
        tile: CardTile,
        itemX: Int,
        itemY: Int,
    ) {
        renderTile(
            drawService,
            tile.image,
            tile.imageList,
            itemX,
            itemY,
        )
    }

    fun renderTile(
        drawService: IDrawService,
        image: CardTileImage,
        imageList: List<CardTileImage>,
        itemX: Int,
        itemY: Int,
    ) {
        if (imageList.size > 1 && image.itemStyle == ItemStyle.ONE_OF) {
            drawService.matrices.push()
            drawService.matrices.translate(itemX.toFloat(), itemY.toFloat(), 0f)
            val scale = (12f / 16f)
            val translate = (4f / scale) / 2f
            drawService.matrices.scale(scale, scale, 1f)

            for (i in 0..2) {
                val imageListImage = imageList.getOrNull(i)
                    ?: continue

                drawService.matrices.push()
                drawService.matrices.translate(translate*i, translate*i, 10f * (3-i))
                val tint = 0.5f + (1f - i/2f)*0.5f
                drawService.setShaderColor(tint, tint, tint, 1f)
                renderTileImage(
                    drawService = drawService,
                    image = imageListImage,
                    itemX = 0,
                    itemY = 0,
                )
                drawService.matrices.pop()
            }

            drawService.clearShaderColor()
            drawService.matrices.pop()
        } else if (imageList.size > 1 && image.itemStyle == ItemStyle.ALL_OF) {
            drawService.matrices.push()
            drawService.matrices.translate(itemX.toFloat(), itemY.toFloat(), 0f)
            val scale = (9f / 16f)
            drawService.matrices.scale(scale, scale, 1f)

            for (i in 0..3) {
                val imageListImage = imageList.getOrNull(i)
                    ?: continue

                renderTileImage(
                    drawService = drawService,
                    image = imageListImage,
                    itemX = (i % 2) * 12,
                    itemY = (i / 2) * 12,
                )
            }

            drawService.matrices.pop()
        } else {
            renderTileImage(drawService, image, itemX, itemY)
        }
    }

    fun renderTileImage(
        drawService: IDrawService,
        image: CardTileImage,
        itemX: Int,
        itemY: Int,
    ) {
        val texture = image.texture?.let { hudState.images[it] }
        val item = image.item
        if (texture != null) {
            drawService.drawDynamicTexture(texture, itemX, itemY, 16, 16)
        } else if (item != null) {
            // always draws centered at z=150... but 3d items can extend past this
            // so any overlays should start at z=300 to be safe
            drawService.drawItemStack(item, itemX, itemY, itemX + itemY * CARD_WIDTH)
        }
    }

    fun renderTileDecorations(
        drawService: IDrawService,
        tile: CardTile,
        itemX: Int,
        itemY: Int,
    ) {
        renderTileDecorations(drawService, tile.image, itemX, itemY)
    }

    fun renderTileDecorations(
        drawService: IDrawService,
        image: CardTileImage,
        itemX: Int,
        itemY: Int,
    ) {
        val texture = image.decorationTexture
            ?.let { Identifier(it) }
            ?: when (image.decoration) {
                Decoration.ADVANCEMENT -> DECORATION_ADVANCEMENT_ID
                Decoration.FORBIDDEN -> DECORATION_FORBIDDEN_ID
                Decoration.FREE_SPACE -> DECORATION_FREE_SPACE_ID
                Decoration.MULTI_ITEM, Decoration.NONE -> null
            }
            ?: return

        drawService.drawGuiTexture(
            texture = texture,
            x = itemX - 2, y = itemY - 2, z = 301,
            u = 0f, v = 0f,
            width = 20, height = 20,
            textureWidth = 20, textureHeight = 20,
        )
    }
}