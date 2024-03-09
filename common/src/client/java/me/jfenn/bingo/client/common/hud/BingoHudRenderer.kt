package me.jfenn.bingo.client.common.hud

import me.jfenn.bingo.client.api.IDrawService
import me.jfenn.bingo.common.map.CardView
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

class BingoHudRenderer {

    companion object {
        const val CARD_WIDTH = 118
        const val CARD_HEIGHT = 128
    }

    private fun isDark(color: Formatting) = when (color) {
        Formatting.BLACK -> true
        Formatting.DARK_BLUE -> true
        Formatting.DARK_GREEN -> true
        Formatting.DARK_AQUA -> true
        Formatting.DARK_RED -> true
        Formatting.DARK_PURPLE -> true
        Formatting.GOLD -> false
        Formatting.GRAY -> false
        Formatting.DARK_GRAY -> true
        Formatting.BLUE -> true
        Formatting.GREEN -> false
        Formatting.AQUA -> false
        Formatting.RED -> true
        Formatting.LIGHT_PURPLE -> false
        Formatting.YELLOW -> false
        Formatting.WHITE -> false
        else -> false
    }

    fun drawCard(
        context: DrawContext,
        service: IDrawService,
        view: CardView,
        x: Int,
        y: Int,
        cardScale: Float,
        mouseX: Int = -1,
        mouseY: Int = -1,
    ) {
        val textRenderer = MinecraftClient.getInstance().textRenderer

        context.matrices.push()
        context.matrices.scale(cardScale, cardScale, cardScale)

        service.drawTexture(
            texture = Identifier("minecraft", "bingo/hud_card"),
            x = x,
            y = y,
            u = 0f,
            v = 0f,
            width = CARD_WIDTH,
            height = CARD_HEIGHT,
            textureWidth = CARD_WIDTH,
            textureHeight = CARD_HEIGHT,
        )

        view.teamColor?.let { color ->
            service.drawTexture(
                texture = Identifier("minecraft", "bingo/hud_frame_${color.name.lowercase()}"),
                x = x, y = y,
                u = 0f, v = 0f,
                width = CARD_WIDTH,
                height = CARD_HEIGHT,
                textureWidth = CARD_WIDTH,
                textureHeight = CARD_HEIGHT,
            )
        }

        val teamNameColor = when (view.teamColor?.let { isDark(it) }) {
            true -> 0xa8ffffff.toInt()
            false -> 0xa8000000.toInt()
            else -> 0xff505050.toInt()
        }
        context.drawText(textRenderer, view.teamName, x + 10, y + 8, teamNameColor, false)

        for (tileX in 0..4) for (tileY in 0..4) {
            val tile = view.tile(tileX, tileY) ?: continue
            val itemX = 10 + x + tileX*20
            val itemY = 20 + y + tileY*20

            when {
                tile.isFlashing -> context.fill(itemX, itemY, itemX + 18, itemY + 18, 0xffb24cd8.toInt())
                tile.isAchieved -> context.fill(itemX, itemY, itemX + 18, itemY + 18, 0xff55FF55.toInt())
            }

            if (tile.isAdvancement) {
                service.drawTexture(
                    texture = Identifier("minecraft", "bingo/image_frame_advancement"),
                    x = itemX - 1, y = itemY - 1,
                    u = 0f, v = 0f,
                    width = 20, height = 20,
                    textureWidth = 20, textureHeight = 20,
                )
            }

            if (tile.isHidden) {
                val texture = tile.itemTier
                    ?.let { Identifier("minecraft", "bingo/image_hidden_${it.name.lowercase()}") }
                    ?: Identifier("minecraft", "bingo/image_hidden")

                service.drawTexture(
                    texture = texture,
                    x = itemX + 1, y = itemY + 1,
                    u = 0f, v = 0f,
                    width = 16, height = 16,
                    textureWidth = 16, textureHeight = 16,
                )
            }

            if (tile.item != null) {
                // always draws centered at z=150... but 3d items can extend past this
                // so any overlays should start at z=300 to be safe
                context.drawItem(tile.item, itemX + 1, itemY + 1, itemX + itemY * CARD_WIDTH)
                // draws the item count + durability bar
                context.drawItemInSlot(textRenderer, tile.item, itemX, itemY)
            }

            if (tile.isAchieved && !tile.isFlashing)
                context.fill(RenderLayer.getGuiOverlay(), itemX, itemY, itemX + 18, itemY + 18, 0x4055FF55)

            if (tile.isMultiItem) {
                service.drawTexture(
                    texture = Identifier("minecraft", "bingo/image_frame_multi"),
                    x = itemX - 1, y = itemY - 1, z = 300,
                    u = 0f, v = 0f,
                    width = 20, height = 20,
                    textureWidth = 20, textureHeight = 20,
                )
            }

            if (tile.isLocked) {
                service.drawTexture(
                    texture = Identifier("minecraft", "bingo/image_frame_locked"),
                    x = itemX - 1, y = itemY - 1, z = 301,
                    u = 0f, v = 0f,
                    width = 20, height = 20,
                    textureWidth = 20, textureHeight = 20,
                )
            }

            for ((i, teamColor) in tile.teamColors.withIndex()) {
                val colorValue = teamColor.colorValue?.or(0xff000000.toInt())
                    ?: continue

                val start = i * 34f / tile.teamColors.size
                val end = (i+1) * 34f / tile.teamColors.size

                if (start < 17f) {
                    context.drawHorizontalLine(itemX + start.toInt(), itemX + 17, itemY, colorValue)
                    context.drawVerticalLine(itemX, itemY + start.toInt(), itemY + 17, colorValue)
                }

                if (end > 17f) {
                    context.drawHorizontalLine(itemX, itemX + end.toInt() - 17, itemY + 17, colorValue)
                    context.drawVerticalLine(itemX + 17, itemY, itemY + end.toInt() - 17, colorValue)
                }
            }

            // Render the hover tooltip last
            if (tile.item != null && mouseX in itemX until itemX+20 && mouseY in itemY until itemY+20) {
                context.drawItemTooltip(textRenderer, tile.item, mouseX, mouseY)
            }
        }

        context.matrices.pop()
    }

}