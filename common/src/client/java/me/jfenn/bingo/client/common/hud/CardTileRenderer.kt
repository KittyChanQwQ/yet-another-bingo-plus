package me.jfenn.bingo.client.common.hud

import me.jfenn.bingo.client.common.hud.BingoHudRenderer.Companion.CARD_WIDTH
import me.jfenn.bingo.client.platform.IDrawService
import me.jfenn.bingo.common.map.CardTile
import me.jfenn.bingo.common.map.CardTileImage
import net.minecraft.util.Identifier

internal class CardTileRenderer(
    private val hudState: BingoHudState,
) {

    private companion object {
        val ADVANCEMENT_FRAME_ID = Identifier.of("minecraft", "bingo/image_frame_advancement")!!
        val FRAME_FREE_SPACE_ID = Identifier.of("minecraft", "bingo/image_frame_free_space")!!
        val FRAME_FORBIDDEN_ID = Identifier.of("minecraft", "bingo/image_frame_forbidden")!!
    }

    fun renderTile(
        drawService: IDrawService,
        tile: CardTile,
        itemX: Int,
        itemY: Int,
    ) {
        val context by drawService::context
        if (tile.imageList.size > 1 && tile.decoration == CardTile.Decoration.MULTI_ITEM) {
            context.matrices.push()
            context.matrices.translate(itemX.toDouble(), itemY.toDouble(), 0.0)
            val scale = (9f / 16f)
            context.matrices.scale(scale, scale, 1f)

            for (i in 0..3) {
                val image = tile.imageList.getOrNull(i)
                    ?: continue

                renderTileImage(
                    drawService = drawService,
                    image = image,
                    itemX = (i % 2) * 12,
                    itemY = (i / 2) * 12,
                )
            }

            context.matrices.pop()
        } else {
            renderTileImage(drawService, tile.image, itemX, itemY)
        }
    }

    private fun renderTileImage(
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
        if (tile.decoration == CardTile.Decoration.ADVANCEMENT) {
            drawService.drawGuiTexture(
                texture = ADVANCEMENT_FRAME_ID,
                x = itemX - 2, y = itemY - 2, z = 300,
                u = 0f, v = 0f,
                width = 20, height = 20,
                textureWidth = 20, textureHeight = 20,
            )
        }

        if (tile.decoration == CardTile.Decoration.FREE_SPACE) {
            drawService.drawGuiTexture(
                texture = FRAME_FREE_SPACE_ID,
                x = itemX - 2, y = itemY - 2, z = 301,
                u = 0f, v = 0f,
                width = 20, height = 20,
                textureWidth = 20, textureHeight = 20,
            )
        }

        if (tile.decoration == CardTile.Decoration.FORBIDDEN) {
            drawService.drawGuiTexture(
                texture = FRAME_FORBIDDEN_ID,
                x = itemX - 2, y = itemY - 2, z = 301,
                u = 0f, v = 0f,
                width = 20, height = 20,
                textureWidth = 20, textureHeight = 20,
            )
        }
    }
}