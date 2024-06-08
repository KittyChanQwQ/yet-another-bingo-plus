package me.jfenn.bingo.client.common.hud

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import me.jfenn.bingo.client.api.IDrawService
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.map.CardView
import me.jfenn.bingo.common.map.Color
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.utils.json
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourceType
import net.minecraft.util.Identifier
import kotlin.jvm.optionals.getOrNull

class BingoHudRenderer {

    companion object {
        const val CARD_WIDTH = 122
        const val CARD_HEIGHT = 132

        val BLACK_A40 = Color.BLACK.copy(a = 64)

        val CARD_COLORS_ID = Identifier.of(MOD_ID_BINGO, "gui/card_colors.json")!!
        val ADVANCEMENT_FRAME_ID = Identifier.of("minecraft", "bingo/image_frame_advancement")!!
        val FRAME_MULTI_ID = Identifier.of("minecraft", "bingo/image_frame_multi")!!
        val FRAME_LOCKED_ID = Identifier.of("minecraft", "bingo/image_frame_locked")!!
    }

    private var cardColors = BingoCardColors()

    fun getItem(
        x: Int,
        y: Int,
        cardScale: Float,
        mouseX: Int,
        mouseY: Int,
    ): Pair<Int, Int>? {
        val startX = x + (11 * cardScale)
        val startY = y + (21 * cardScale)

        val tileX = (mouseX - startX)
            .takeIf { it >= 0f }
            ?.div(20 * cardScale)
            ?.toInt()
            ?.takeIf { it in 0..4 }
            ?: return null

        val tileY = (mouseY - startY)
            .takeIf { it >= 0f }
            ?.div(20 * cardScale)
            ?.toInt()
            ?.takeIf { it in 0..4 }
            ?: return null

        return Pair(tileX, tileY)
    }

    fun drawCard(
        context: DrawContext,
        service: IDrawService,
        view: CardView,
        x: Int,
        y: Int,
        z: Int = 0,
        cardScale: Float,
        mouseX: Int = -1,
        mouseY: Int = -1,
        showTeamOutlines: Boolean = true,
        isFocused: Boolean = false,
        isMouseOver: Boolean = false,
    ) {
        val colors = cardColors.teamColors[view.teamKey ?: BingoTeamKey("preview")] ?: BingoCardColors.DEFAULTS
        val textRenderer = MinecraftClient.getInstance().textRenderer

        context.matrices.push()
        context.matrices.translate(x.toFloat(), y.toFloat(), z.toFloat())
        context.matrices.scale(cardScale, cardScale, cardScale)

        // Draw a translucent background behind all card items
        context.fill(10, 20, 10 + 5*20, 20 + 5*20, colors.backgroundColor.asIntWithAlpha)

        val cardName = view.teamKey?.label ?: "preview"
        service.drawTexture(
            texture = Identifier.of("minecraft", "bingo/card_$cardName")!!,
            x = 0, y = 0,
            u = 0f, v = 0f,
            width = CARD_WIDTH,
            height = CARD_HEIGHT,
            textureWidth = CARD_WIDTH,
            textureHeight = CARD_HEIGHT,
        )

        context.drawText(textRenderer, view.teamName, 12, 10, colors.textColor.asIntWithAlpha, false)

        for (tileX in 0..4) for (tileY in 0..4) {
            val tile = view.tile(tileX, tileY) ?: continue
            val itemX = 12 + tileX*20
            val itemY = 22 + tileY*20

            when {
                tile.isFlashing -> context.fill(itemX, itemY, itemX + 18, itemY + 18, colors.tileFlashingColor.asInt)
                tile.isAchieved -> context.fill(itemX, itemY, itemX + 18, itemY + 18, colors.tileAchievedColor.asInt)
            }

            if (tile.isAdvancement) {
                service.drawTexture(
                    texture = ADVANCEMENT_FRAME_ID,
                    x = itemX - 1, y = itemY - 1,
                    u = 0f, v = 0f,
                    width = 20, height = 20,
                    textureWidth = 20, textureHeight = 20,
                )
            }

            if (tile.isHidden) {
                val texture = tile.itemTier
                    ?.let { Identifier.of("minecraft", "bingo/image_hidden_${it.name.lowercase()}")!! }
                    ?: Identifier.of("minecraft", "bingo/image_hidden")!!

                service.drawTexture(
                    texture = texture,
                    x = itemX + 1, y = itemY + 1,
                    u = 0f, v = 0f,
                    width = 16, height = 16,
                    textureWidth = 16, textureHeight = 16,
                )
            }

            tile.item?.stack?.let { stack ->
                // always draws centered at z=150... but 3d items can extend past this
                // so any overlays should start at z=300 to be safe
                context.drawItem(stack, itemX + 1, itemY + 1, itemX + itemY * CARD_WIDTH)
                // draws the item count + durability bar
                context.drawItemInSlot(textRenderer, stack, itemX, itemY)
            }

            if (tile.isAchieved && !tile.isFlashing && mouseX == -1 && mouseY == -1) {
                context.fill(
                    RenderLayer.getGuiOverlay(),
                    itemX,
                    itemY,
                    itemX + 18,
                    itemY + 18,
                    colors.tileAchievedColor.copy(a = colors.tileAchievedColor.a / 4).asIntWithAlpha
                )
            }

            if (tile.isMultiItem) {
                service.drawTexture(
                    texture = FRAME_MULTI_ID,
                    x = itemX - 1, y = itemY - 1, z = 300,
                    u = 0f, v = 0f,
                    width = 20, height = 20,
                    textureWidth = 20, textureHeight = 20,
                )
            }

            if (tile.isLocked) {
                service.drawTexture(
                    texture = FRAME_LOCKED_ID,
                    x = itemX - 1, y = itemY - 1, z = 301,
                    u = 0f, v = 0f,
                    width = 20, height = 20,
                    textureWidth = 20, textureHeight = 20,
                )
            }

            if (showTeamOutlines) for ((i, team) in tile.teamKeys.withIndex()) {
                val color = cardColors.teamColors[team]?.outlineColor ?: Color.TRANSPARENT
                val colorValue = color.asInt
                val colorValueDark = color.mix(BLACK_A40).asInt

                val start = i * 38f / tile.teamKeys.size
                val end = (i+1) * 38f / tile.teamKeys.size

                if (start < 19f) {
                    val lineStart = start.toInt()
                    val lineEnd = end.toInt().coerceAtMost(19)
                    context.drawHorizontalLine((itemX-1) + lineStart, (itemX-1) + lineEnd, (itemY-1), colorValue)
                    context.drawVerticalLine((itemX-1), (itemY-1) + lineStart, (itemY-1) + lineEnd, colorValue)
                }

                if (end > 19f) {
                    val lineStart = (start.toInt() - 19).coerceAtLeast(0)
                    val lineEnd = end.toInt() - 19
                    context.drawHorizontalLine((itemX-1) + lineStart, (itemX-1) + lineEnd, (itemY-1) + 19, colorValueDark)
                    context.drawVerticalLine((itemX-1) + 19, (itemY-1) + lineStart, (itemY-1) + lineEnd, colorValueDark)
                }

                val start2 = i * 34f / tile.teamKeys.size
                val end2 = (i+1) * 34f / tile.teamKeys.size

                if (start2 < 17f) {
                    val lineStart = start2.toInt()
                    val lineEnd = end2.toInt().coerceAtMost(17)
                    context.drawHorizontalLine((itemX) + lineStart, (itemX) + lineEnd, itemY, colorValueDark)
                    context.drawVerticalLine(itemX, (itemY) + lineStart, (itemY) + lineEnd, colorValueDark)
                }

                if (end2 > 17f) {
                    val lineStart = (start2.toInt() - 17).coerceAtLeast(0)
                    val lineEnd = end2.toInt() - 17
                    context.drawHorizontalLine((itemX) + lineStart, (itemX) + lineEnd, (itemY) + 17, colorValue)
                    context.drawVerticalLine((itemX) + 17, (itemY) + lineStart, (itemY) + lineEnd, colorValue)
                }
            }
        }

        if (isFocused) {
            service.drawTexture(
                texture = Identifier("minecraft", "bingo/card_outline"),
                x = 0, y = 0,
                u = 0f, v = 0f,
                width = CARD_WIDTH,
                height = CARD_HEIGHT,
                textureWidth = CARD_WIDTH,
                textureHeight = CARD_HEIGHT,
            )
        }

        context.matrices.pop()

        if (isMouseOver) {
            // Render the hover tooltips last, without the matrix translate/transform
            val (tileX, tileY) = getItem(x, y, cardScale, mouseX, mouseY) ?: return
            val tile = view.tile(tileX, tileY) ?: return

            tile.item?.stack?.let { stack ->
                context.matrices.push()
                context.matrices.translate(0f, 0f, 2000f)
                context.drawItemTooltip(textRenderer, stack, mouseX, mouseY)
                context.matrices.pop()
            }
        }
    }

    init {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(object : SimpleSynchronousResourceReloadListener {
            override fun reload(manager: ResourceManager) {
                this@BingoHudRenderer.cardColors = manager.getResource(CARD_COLORS_ID)
                    .getOrNull()
                    ?.inputStream
                    ?.use {
                        @OptIn(ExperimentalSerializationApi::class)
                        json.decodeFromStream<BingoCardColors>(it)
                    }
                    ?: BingoCardColors()
            }

            override fun getFabricId(): Identifier = Identifier.of(MOD_ID_BINGO, "hud_renderer")!!
        })
    }

}