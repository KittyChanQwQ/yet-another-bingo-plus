package me.jfenn.bingo.client.common.hud

import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import me.jfenn.bingo.client.common.utils.Interpolate
import me.jfenn.bingo.client.platform.renderer.IDrawService
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.map.CardTile
import me.jfenn.bingo.common.map.CardView
import me.jfenn.bingo.common.map.Color
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.json
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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class BingoHudRenderer(
    private val text: TextProvider,
    private val state: BingoHudState,
    private val cardTileRenderer: CardTileRenderer,
) {

    companion object {
        const val CARD_WIDTH = 122
        const val CARD_HEIGHT = 132
        const val TOOLTIP_MAX_WIDTH = 200

        val BLACK_A40 = Color.BLACK.copy(a = 64)

        val CARD_COLORS_ID = Identifier.of(MOD_ID_BINGO, "gui/card_colors.json")!!
        val FRAME_LOCKED_ID = Identifier.of("minecraft", "bingo/image_frame_locked")!!
        val IMAGE_HIDDEN_ID = Identifier.of("minecraft", "bingo/image_hidden")!!
        val CARD_TOOLTIP_ID = Identifier.of("minecraft", "bingo/tooltip")!!

        val SHUFFLE_DURATION = 280.milliseconds

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

    var shufflePositions = (0 until 25).toList()

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
        service: IDrawService,
        view: CardView,
        showTeamOutlines: Boolean = true,
        showFlashingTiles: Boolean = true,
        isGui: Boolean = false,
    ) {
        val context by service::context

        val colors = cardColors.getTeamColors(view.teamKey ?: BingoTeamKey("preview"), view.display.teamColor)

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

        view.display.teamName?.let { teamName ->
            // Get or create clientTeamName (truncated to fit on the card)
            val clientTeamName = view.display.clientTeamName
                ?: service.font.truncate(teamName, 70)
                    .also { view.display.clientTeamName = it }
            service.drawText(clientTeamName, colors.textX, colors.textY, colors.textColor.asIntWithAlpha, false)
        }

        for (tileIndex in 0 until 25) {
            val tileX = tileIndex % 5
            val tileY = tileIndex / 5

            val tile = view.tiles.getOrNull(tileIndex) ?: continue

            val isReplacing = showFlashingTiles && shouldRenderReplacing(tile)
            if (isReplacing) continue

            val interpolateFrom = shufflePositions[tileIndex % shufflePositions.size]
                .let { Pair(it % 5, it / 5) }
                .let { (x, y) -> Pair(12 + x*20, 22 + y*20) }
            val interpolateTo = Pair(12 + tileX*20, 22 + tileY*20)
            val interpolateX = Interpolate(
                from = interpolateFrom.first.toFloat(),
                to = interpolateTo.first.toFloat(),
                duration = SHUFFLE_DURATION,
                startedAt = view.shuffledAt,
            )
            val interpolateY = Interpolate(
                from = interpolateFrom.second.toFloat(),
                to = interpolateTo.second.toFloat(),
                duration = SHUFFLE_DURATION,
                startedAt = view.shuffledAt,
            )

            val itemX = interpolateX.get(Interpolate.Easing.IN_OUT, state.now)
            val itemY = interpolateY.get(Interpolate.Easing.IN_OUT, state.now)

            // BEGIN: TILE DRAW
            service.matrices.push()
            service.matrices.translate(itemX, itemY, 0f)

            val isFlashing = showFlashingTiles && shouldRenderFlashing(tile)
            when {
                isFlashing -> context.fill(0, 0, 18, 18, colors.tileFlashingColor.asInt)
                tile.isAchieved -> context.fill(0, 0, 18, 18, colors.tileAchievedColor.asInt)
                tile.progress >= (1/18f) -> context.fill(
                    0, (18 * (1f - tile.progress)).toInt().coerceIn(0, 18),
                    18, 18,
                    colors.tileProgressColor.asInt
                )
            }

            if (tile.isHidden) {
                val texture = tile.itemTier
                    ?.let { Identifier.of("minecraft", "bingo/image_hidden_${it.name.lowercase()}")!! }
                    ?: IMAGE_HIDDEN_ID

                service.drawGuiTexture(
                    texture = texture,
                    x = 1, y = 1,
                    u = 0f, v = 0f,
                    width = 16, height = 16,
                    textureWidth = 16, textureHeight = 16,
                )
            }

            cardTileRenderer.renderTile(service, tile, 1, 1)

            if (tile.isAchieved && !isFlashing && !isGui) {
                context.fill(
                    RenderLayer.getGuiOverlay(),
                    0,
                    0,
                    18,
                    18,
                    colors.tileAchievedColor.copy(a = colors.tileAchievedColor.a / 4).asIntWithAlpha
                )
            }

            cardTileRenderer.renderTileDecorations(service, tile, 1, 1)

            if (tile.isLocked) {
                service.drawGuiTexture(
                    texture = FRAME_LOCKED_ID,
                    x = -1, y = -1, z = 301,
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
                    context.drawHorizontalLine((-1) + lineStart, (-1) + lineEnd, (-1), colorValue)
                    context.drawVerticalLine((-1), (-1) + lineStart, (-1) + lineEnd, colorValue)
                }

                if (end > 19f) {
                    val lineStart = (start.toInt() - 19).coerceAtLeast(0)
                    val lineEnd = end.toInt() - 19
                    context.drawHorizontalLine((-1) + lineStart, (-1) + lineEnd, (-1) + 19, colorValueDark)
                    context.drawVerticalLine((-1) + 19, (-1) + lineStart, (-1) + lineEnd, colorValueDark)
                }

                val start2 = i * 34f / tile.teamKeys.size
                val end2 = (i+1) * 34f / tile.teamKeys.size

                if (start2 < 17f) {
                    val lineStart = start2.toInt()
                    val lineEnd = end2.toInt().coerceAtMost(17)
                    context.drawHorizontalLine(lineStart, lineEnd, 0, colorValueDark)
                    context.drawVerticalLine(0, lineStart, lineEnd, colorValueDark)
                }

                if (end2 > 17f) {
                    val lineStart = (start2.toInt() - 17).coerceAtLeast(0)
                    val lineEnd = end2.toInt() - 17
                    context.drawHorizontalLine(lineStart, lineEnd, 17, colorValue)
                    context.drawVerticalLine(17, lineStart, lineEnd, colorValue)
                }
            }

            // END: TILE DRAW
            service.matrices.pop()
        }
    }

    fun drawAll(
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

        service.matrices.push()
        service.matrices.translate(x, y, z.toFloat())
        service.matrices.scale(cardScale, cardScale, 1f)

        if (isGui) {
            drawCard(
                service = service,
                view = view,
                showTeamOutlines = showTeamOutlines,
                showFlashingTiles = showFlashingTiles,
                isGui = isGui,
            )
        }

        if (isWinner) {
            service.matrices.push()
            service.matrices.translate(90f, -4f, 1000f)
            service.matrices.multiply(Quaternionf(AxisAngle4f(.25f, 0f, 0f, 1f)))

            service.drawText(text.string(StringKey.CardWinner), 0, 0, getWinColor(state.now), true)

            service.matrices.pop()
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

        service.matrices.pop()

        // Render the team's player list if the title is hovered over
        if (
            isMouseOver &&
            mouseX in x.toInt()..(x + CARD_WIDTH*0.7*cardScale).toInt() &&
            mouseY in y.toInt()..(y+20*cardScale).toInt() &&
            view.display.players.isNotEmpty()
        ) {
            service.matrices.push()
            service.matrices.translate(0f, 0f, z + 2000f)
            context.drawTooltip(service.textRenderer, view.display.players.map { Text.literal(it.name) }, mouseX, mouseY)
            service.matrices.pop()
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
        val tooltip = tile.clientTooltip ?: buildList {
            tile.name?.let { add(it) }
            addAll(
                tile.lore.flatMapIndexed { i, lore ->
                    val isLast = i >= tile.lore.size - 1 // the last item (tile info) should never be wrapped
                    if (isLast) listOf(lore)
                    else service.font.wrapLines(lore, TOOLTIP_MAX_WIDTH)
                }
            )
        }.also {
            tile.clientTooltip = it
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
            service.matrices.push()
            service.matrices.translate(0f, 0f, z + 2000f)

            if (tile.imageList.isNotEmpty()) {
                val tooltipWidth = tooltip.maxOf { service.font.getTextWidth(it) }
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

            context.drawTooltip(service.textRenderer, tooltip.map { it.value }, mouseX, mouseY)
            service.matrices.pop()
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