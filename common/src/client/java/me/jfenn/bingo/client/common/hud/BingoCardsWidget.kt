package me.jfenn.bingo.client.common.hud

import me.jfenn.bingo.client.common.utils.Interpolate
import me.jfenn.bingo.client.platform.IDrawService
import me.jfenn.bingo.client.platform.IDrawServiceFactory
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.map.CardTile
import me.jfenn.bingo.common.map.CardView
import me.jfenn.bingo.common.team.BingoTeamKey
import net.minecraft.client.gui.screen.Screen
import org.koin.core.Koin
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.time.Duration

internal class BingoCardsWidget(
    koin: Koin,

    var x: Float,
    var y: Float,
    var width: Int,
    var height: Int,

    var interpolateKey: BingoTeamKey?,
    interpolateFromX: Float = 0f,
    interpolateFromY: Float = 0f,
    interpolateFromScale: Float = 1f,
    interpolateDuration: Duration = Duration.ZERO,

    val showFlashingTiles: Boolean,
    var views: Collection<CardView> = emptyList(),
    var winner: BingoTeamKey? = null,

    val onTileClick: (CardTile) -> Boolean,
    val onViewClick: ((CardView) -> Boolean)?,

    private val screen: Screen,
    private val drawServiceFactory: IDrawServiceFactory = koin.get(),
    private val renderer: BingoHudRenderer = koin.get(),
    private val config: BingoConfig = koin.get()
) {

    val interpolateX = Interpolate(
        from = interpolateFromX,
        to = 0f,
        duration = interpolateDuration,
    )

    val interpolateY = Interpolate(
        from = interpolateFromY,
        to = 0f,
        duration = interpolateDuration,
    )

    val interpolateScale = Interpolate(
        from = interpolateFromScale,
        to = 1f,
        duration = interpolateDuration,
    )

    private var targetScrollX: Float = 0f
    private var scrollX: Float = 0f // 0-1

    private val cardsAreaMargin get() = screen.width * 2/10
    private val cardsAreaWidth get() = screen.width - cardsAreaMargin*2
    private val cardsTotalWidth get() =  views.size * BingoHudRenderer.CARD_WIDTH

    fun viewsWithPositions(
        callback: (view: CardView, x: Float, y: Float, z: Int) -> Unit
    ) {
        if (views.isEmpty()) return

        val xMin = x - (width/2)
        val xMax = x + (width/2) - BingoHudRenderer.CARD_WIDTH

        val widthDiff = (cardsTotalWidth - cardsAreaWidth).coerceAtLeast(0) / 2

        val scrollCenterX = (scrollX - 0.5f) * -2f
        val widthOffset = scrollCenterX * widthDiff

        val screenX = x - cardsTotalWidth/2
        val screenY = y - BingoHudRenderer.CARD_HEIGHT/2

        val zOrder = views.indices.toList()
            .sortedBy { i ->
                val xOffset = i * BingoHudRenderer.CARD_WIDTH + widthOffset + (BingoHudRenderer.CARD_WIDTH/2)
                val x = screenX + xOffset.roundToInt()
                val distanceToCenter = x - this.x
                distanceToCenter.absoluteValue
            }

        for ((i, view) in views.withIndex()) {
            val xOffset = i * BingoHudRenderer.CARD_WIDTH + widthOffset

            val x = screenX + xOffset
            val y = screenY

            val zIndex = zOrder.indexOf(i)
            val z = 302 * (views.size - zIndex)

            val xCoerced = when {
                x < xMin -> x.coerceAtLeast(xMin - 4f * (views.size - i - 1))
                x > xMax -> x.coerceAtMost(xMax + 4f * i)
                else -> x
            }

            val (currentX, currentY) = when {
                view.teamKey == interpolateKey -> {
                    interpolateX.to = xCoerced
                    interpolateY.to = y
                    Pair(
                        interpolateX.get(Interpolate.Easing.IN_OUT),
                        interpolateY.get(Interpolate.Easing.IN_OUT)
                    )
                }
                else -> Pair(xCoerced, y)
            }

            callback(view, currentX, currentY, z)
        }
    }

    private fun getHoveredView(mouseX: Int, mouseY: Int): CardView? {
        var hoveredView: CardView? = null
        var hoveredZ = 0
        viewsWithPositions { view, x, y, z ->
            if (
                mouseX in x.toInt()..(x.toInt() + BingoHudRenderer.CARD_WIDTH) &&
                mouseY in y.toInt()..(y.toInt() + BingoHudRenderer.CARD_HEIGHT) &&
                z > hoveredZ
            ) {
                hoveredView = view
                hoveredZ = z
            }
        }

        return hoveredView
    }

    fun render(drawService: IDrawService, mouseX: Int, mouseY: Int, delta: Float) {
        val context by drawService::context
        if (isInBounds(x.toInt(), mouseY)) {
            val boundaryMin = x - width*.5f
            val boundaryMax = x + width*.5f
            val baseVelocity = 0.01f / (1 + views.size)
            val velocity = when {
                mouseX < boundaryMin -> (mouseX - boundaryMin) * baseVelocity
                mouseX > boundaryMax -> (mouseX - boundaryMax) * baseVelocity
                else -> 0f
            }
            targetScrollX = (targetScrollX + velocity * delta).coerceIn(0f, 1f)
        }

        // Animate scrollX towards targetScrollX
        scrollX = (scrollX * 3 + targetScrollX) / 4
        if (!targetScrollX.isFinite() || !scrollX.isFinite()) {
            targetScrollX = 0f
            scrollX = 0f
        }

        val hoveredView = getHoveredView(mouseX, mouseY)

        viewsWithPositions { view, x, y, z ->
            val isHovered = hoveredView != null && hoveredView.teamKey == view.teamKey
            val isSingleView = views.size <= 1
            renderer.drawCard(
                context,
                drawService,
                view,
                x = x,
                y = y,
                z = z,
                cardScale = interpolateScale.get(Interpolate.Easing.IN_OUT),
                mouseX = mouseX,
                mouseY = mouseY,
                showTeamOutlines = isSingleView && config.client.cardTeamOutlines,
                showFlashingTiles = showFlashingTiles,
                isFocused = isHovered && !isSingleView && onViewClick != null,
                isMouseOver = interpolateX.isDone() && isHovered,
                isGui = true, // uses an alternative card texture with a shadow to reduce clutter when overlapped
                isWinner = winner != null && winner == view.teamKey,
            )
        }

        if (cardsTotalWidth > cardsAreaWidth) {
            val barX = x.toInt() - (width/2)
            val barY = y.toInt() + (height/2) + 3

            val barHeight = 2
            val barFilledWidth = width * cardsAreaWidth / cardsTotalWidth.coerceAtLeast(1)
            val barOffsetMax = width - barFilledWidth
            val barOffsetScrolled = (scrollX * barOffsetMax).roundToInt()

            val barColorBg = 0x80000000.toInt()
            val barColorFilled = 0xff6b6b85.toInt()

            context.fill(barX, barY, barX + barOffsetScrolled, barY + barHeight, 0, barColorBg)
            context.fill(barX + barOffsetScrolled, barY, barX + barOffsetScrolled + barFilledWidth, barY + barHeight, 0, barColorFilled)
            context.fill(barX + barOffsetScrolled + barFilledWidth, barY, barX + width, barY + barHeight, 0, barColorBg)
        }
    }

    private fun isInBounds(mouseX: Int, mouseY: Int): Boolean {
        return mouseX in (x - width/2).toInt()..(x + width/2).toInt()
                && mouseY in (y - height/2).toInt()..(y + height/2).toInt()
    }

    fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (isInBounds(mouseX.toInt(), mouseY.toInt())) {
            targetScrollX = (targetScrollX + amount.toFloat() * -0.5f / views.size.coerceAtLeast(1)).coerceIn(0f, 1f)
            return true
        } else {
            return false
        }
    }

    fun onMouseClicked(mouseX: Double, mouseY: Double): Boolean {
        val hoveredView = getHoveredView(mouseX.toInt(), mouseY.toInt()) ?: return false

        var isHandled = false

        viewsWithPositions { view, x, y, _ ->
            if (hoveredView.teamKey != view.teamKey) return@viewsWithPositions
            val (tileX, tileY) = renderer.getItem(
                x = x.toInt(),
                y = y.toInt(),
                cardScale = interpolateScale.get(Interpolate.Easing.IN_OUT),
                mouseX = mouseX.toInt(),
                mouseY = mouseY.toInt(),
            ) ?: return@viewsWithPositions
            val tile = view.tile(tileX, tileY) ?: return@viewsWithPositions

            isHandled = onTileClick(tile)
        }

        if (!isHandled) {
            isHandled = onViewClick?.invoke(hoveredView) ?: false
        }

        return isHandled
    }

}