package me.jfenn.bingo.client.common.hud

import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import me.jfenn.bingo.client.platform.IDrawService
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.map.CardTile
import me.jfenn.bingo.common.map.CardView
import me.jfenn.bingo.common.map.Color
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.json
import me.jfenn.bingo.common.utils.wrap
import me.jfenn.bingo.generated.StringKey
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner
import net.minecraft.client.render.RenderLayer
import net.minecraft.resource.ResourceManager
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.joml.AxisAngle4f
import org.joml.Quaternionf
import kotlin.jvm.optionals.getOrNull
import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds

internal class BingoHudRenderer(
    private val text: TextProvider,
    private val state: BingoHudState,
    private val cardTileRenderer: CardTileRenderer,
) {

    companion object {
        const val CARD_WIDTH = 122
        const val CARD_HEIGHT = 132
        const val TOOLTIP_MAX_LENGTH = 30

        val BLACK_A40 = Color.BLACK.copy(a = 64)

        val CARD_COLORS_ID = Identifier.of(MOD_ID_BINGO, "gui/card_colors.json")!!
        val FRAME_LOCKED_ID = Identifier.of("minecraft", "bingo/image_frame_locked")!!
        val IMAGE_HIDDEN_ID = Identifier.of("minecraft", "bingo/image_hidden")!!
        val CARD_TOOLTIP_ID = Identifier.of("minecraft", "bingo/card_tooltip")!!

        private val winColors = listOf(
            0xFF_F5C0A3.toInt(),
            0xFF_A3F5A3.toInt(),
            0xFF_A3F5C7.toInt(),
            0xFF_A3ABF5.toInt(),
            0xFF_CCA3F5.toInt(),
            0xFF_F5A3D6.toInt(),
        )

        fun getWinColor(now: Instant): Int {
            val winIndex = now.epochSeconds % winColors.size
            return winColors[winIndex.toInt()]
        }
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

    private fun shouldRenderReplacing(tile: CardTile): Boolean {
        val updatedAt = tile.updatedAt ?: return false
        val timeSinceUpdated = state.now - updatedAt
        // "== false" asserts that the tile was sent from card_tile_v6 which does not send isFlashing timed with server ticks
        return tile.isFlashing == false &&
                timeSinceUpdated < 4.seconds &&
                (timeSinceUpdated.inWholeMilliseconds / 500L) % 2L == 0L
    }

    private fun shouldRenderFlashing(tile: CardTile): Boolean {
        val isFlashRender = (state.now.toEpochMilliseconds() / 500L) % 2L == 0L
        return tile.isFlashing?.let { isFlashRender && it } ?: tile.isFlashingOnMap
    }

    fun drawCard(
        context: DrawContext,
        service: IDrawService,
        view: CardView,
        x: Float,
        y: Float,
        z: Int = 0,
        cardScale: Float,
        mouseX: Int = -1,
        mouseY: Int = -1,
        showTeamOutlines: Boolean = true,
        showFlashingTiles: Boolean = true,
        isFocused: Boolean = false,
        isMouseOver: Boolean = false,
        isGui: Boolean = false,
        isWinner: Boolean = false,
    ) {
        val colors = cardColors.getTeamColors(view.teamKey ?: BingoTeamKey("preview"), view.display.teamColor)

        context.matrices.push()
        context.matrices.translate(x, y, z.toFloat())
        context.matrices.scale(cardScale, cardScale, 1f)

        // Draw a translucent background behind all card items
        context.fill(10, 20, 10 + 5*20, 20 + 5*20, colors.backgroundColor.asIntWithAlpha)

        service.drawGuiTexture(
            texture = if (isGui) colors.cardTextureGui else colors.cardTexture,
            x = 0, y = 0,
            u = 0f, v = 0f,
            width = CARD_WIDTH,
            height = CARD_HEIGHT,
            textureWidth = CARD_WIDTH,
            textureHeight = CARD_HEIGHT,
        )

        context.drawText(service.textRenderer, view.display.teamName, colors.textX, colors.textY, colors.textColor.asIntWithAlpha, false)

        if (isWinner) {
            context.matrices.push()
            context.matrices.translate(90.0, -4.0, 1000.0)
            context.matrices.multiply(Quaternionf(AxisAngle4f(.25f, 0f, 0f, 1f)))

            context.drawText(service.textRenderer, text.string(StringKey.CardWinner), 0, 0, getWinColor(state.now), true)

            context.matrices.pop()
        }

        for (tileX in 0..4) for (tileY in 0..4) {
            val tile = view.tile(tileX, tileY) ?: continue
            val itemX = 12 + tileX*20
            val itemY = 22 + tileY*20

            val isFlashing = showFlashingTiles && shouldRenderFlashing(tile)
            when {
                isFlashing -> context.fill(itemX, itemY, itemX + 18, itemY + 18, colors.tileFlashingColor.asInt)
                tile.isAchieved -> context.fill(itemX, itemY, itemX + 18, itemY + 18, colors.tileAchievedColor.asInt)
                tile.progress >= (1/18f) -> context.fill(
                    itemX, itemY + (18 * (1f - tile.progress)).toInt().coerceIn(0, 18),
                    itemX + 18, itemY + 18,
                    colors.tileProgressColor.asInt
                )
            }

            if (tile.isHidden) {
                val texture = tile.itemTier
                    ?.let { Identifier.of("minecraft", "bingo/image_hidden_${it.name.lowercase()}")!! }
                    ?: IMAGE_HIDDEN_ID

                service.drawGuiTexture(
                    texture = texture,
                    x = itemX + 1, y = itemY + 1,
                    u = 0f, v = 0f,
                    width = 16, height = 16,
                    textureWidth = 16, textureHeight = 16,
                )
            }

            val isReplacing = showFlashingTiles && shouldRenderReplacing(tile)
            if (isReplacing) continue

            cardTileRenderer.renderTile(service, tile, itemX + 1, itemY + 1)

            if (tile.isAchieved && !isFlashing && mouseX == -1 && mouseY == -1) {
                context.fill(
                    RenderLayer.getGuiOverlay(),
                    itemX,
                    itemY,
                    itemX + 18,
                    itemY + 18,
                    colors.tileAchievedColor.copy(a = colors.tileAchievedColor.a / 4).asIntWithAlpha
                )
            }

            cardTileRenderer.renderTileDecorations(service, tile, itemX + 1, itemY + 1)

            if (tile.isLocked) {
                service.drawGuiTexture(
                    texture = FRAME_LOCKED_ID,
                    x = itemX - 1, y = itemY - 1, z = 301,
                    u = 0f, v = 0f,
                    width = 20, height = 20,
                    textureWidth = 20, textureHeight = 20,
                )
            }

            if (showTeamOutlines) for ((i, team) in tile.teamKeys.withIndex()) {
                val color = cardColors.getTeamColors(team, view.display.teamColor).outlineColor
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
            service.drawGuiTexture(
                texture = colors.cardTextureOutline,
                x = 0, y = 0,
                u = 0f, v = 0f,
                width = CARD_WIDTH,
                height = CARD_HEIGHT,
                textureWidth = CARD_WIDTH,
                textureHeight = CARD_HEIGHT,
            )
        }

        context.matrices.pop()

        // Render the team's player list if the title is hovered over
        if (
            isMouseOver &&
            mouseX in x.toInt()..(x + CARD_WIDTH*0.7*cardScale).toInt() &&
            mouseY in y.toInt()..(y+20*cardScale).toInt() &&
            view.display.players.isNotEmpty()
        ) {
            context.matrices.push()
            context.matrices.translate(0f, 0f, z + 2000f)
            context.drawTooltip(service.textRenderer, view.display.players.map { Text.literal(it.name) }, mouseX, mouseY)
            context.matrices.pop()
        }

        if (isMouseOver) {
            // Render the hover tooltips last, without the matrix translate/transform
            val (tileX, tileY) = getItem(x.toInt(), y.toInt(), cardScale, mouseX, mouseY) ?: return
            val tile = view.tile(tileX, tileY) ?: return

            drawTooltip(service, tile, mouseX, mouseY, z)
        }
    }

    private fun drawTooltip(
        service: IDrawService,
        tile: CardTile,
        mouseX: Int,
        mouseY: Int,
        z: Int,
    ) {
        val context by service::context
        val tooltip = buildList {
            tile.name?.let { add(it) }
            addAll(
                tile.lore.flatMapIndexed { i, lore ->
                    val isLast = i >= tile.lore.size - 1 // the last item (tile info) should never be wrapped
                    if (isLast) listOf(lore)
                    else lore.wrap(TOOLTIP_MAX_LENGTH)
                }
            )
        }

        val rows = when (tile.imageList.size) {
            0 -> 0
            in 1..5 -> 1
            in 6..12 -> 2
            in 13..21 -> 3
            in 22..28 -> 4
            else -> 5
        }
        val columns = ceil(tile.imageList.size / rows.toFloat()).toInt()

        if (tooltip.isNotEmpty()) {
            context.matrices.push()
            context.matrices.translate(0f, 0f, z + 2000f)

            if (tile.imageList.isNotEmpty()) {
                val tooltipWidth = tooltip.maxOf { service.textRenderer.getWidth(it) }
                val tooltipHeight = 10 * tooltip.size // hardcoded in OrderedTextTooltipComponent
                val componentHeight = 14 + 20 * rows
                val componentWidth = 14 + 20 * columns

                val tooltipPosition = HoveredTooltipPositioner.INSTANCE.getPosition(
                    context.scaledWindowWidth,
                    context.scaledWindowHeight,
                    mouseX,
                    mouseY,
                    tooltipWidth,
                    tooltipHeight,
                )

                val x = tooltipPosition.x() - 4
                val y = when {
                    tooltipPosition.y() - componentHeight - 6 > 0 -> tooltipPosition.y() - componentHeight - 6
                    else -> tooltipPosition.y() + tooltipHeight + 6
                }

                service.drawNinePatch(
                    texture = CARD_TOOLTIP_ID,
                    x = x, y = y,
                    sliceSize = 7,
                    width = componentWidth,
                    height = componentHeight,
                    textureWidth = 34,
                    textureHeight = 34,
                )

                tile.imageList.forEachIndexed { index, cardTileImage ->
                    val row = index / columns
                    val col = index % columns
                    cardTileRenderer.renderTileImage(
                        drawService = service,
                        image = cardTileImage,
                        itemX = x + 9 + 20 * col,
                        itemY = y + 9 + 20 * row,
                    )
                }
            }

            context.drawTooltip(service.textRenderer, tooltip, mouseX, mouseY)
            context.matrices.pop()
        }
    }

    fun loadCardColors(manager: ResourceManager) {
        this@BingoHudRenderer.cardColors = manager.getResource(CARD_COLORS_ID)
            .getOrNull()
            ?.inputStream
            ?.use {
                @OptIn(ExperimentalSerializationApi::class)
                json.decodeFromStream<BingoCardColors>(it)
            }
            ?: BingoCardColors()
    }

}