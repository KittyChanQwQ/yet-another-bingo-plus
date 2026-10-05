package me.jfenn.bingo.client.common.hud.screen

import me.jfenn.bingo.client.common.hud.card.ClientCardBufferRenderer
import me.jfenn.bingo.client.common.hud.card.ClientCardRenderer
import me.jfenn.bingo.client.common.state.ClientCard
import me.jfenn.bingo.client.common.state.ClientCardBase
import me.jfenn.bingo.client.common.utils.Interpolate
import me.jfenn.bingo.client.platform.renderer.IDrawService
import me.jfenn.bingo.client.platform.screen.IDrawable
import me.jfenn.bingo.client.platform.screen.IKeyInput
import me.jfenn.bingo.common.map.CardTile
import me.jfenn.bingo.common.team.BingoTeamKey
import org.koin.core.Koin
import java.time.Duration
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

internal class BingoCardsWidget(
    koin: Koin,

    var x: Float = 0f,
    var y: Float = 0f,
    var width: Int = 0,
    var height: Int = 0,

    var interpolateKey: BingoTeamKey?,
    interpolateFromX: Float = 0f,
    interpolateFromY: Float = 0f,
    interpolateFromScale: Float = 1f,
    interpolateDuration: Duration = Duration.ZERO,

    var views: Collection<ClientCardBase> = emptyList(),
    var winner: BingoTeamKey? = null,

    val onTileClick: (CardTile) -> Boolean,
    val onViewClick: ((ClientCard) -> Boolean)?,

    private val renderer: ClientCardRenderer = koin.get(),
) : IDrawable {

    val interpolateX = Interpolate(
        from = interpolateFromX,
        to = 0f,
        duration = interpolateDuration,
        easing = Interpolate.Easing.EXP_OUT,
    )

    val interpolateY = Interpolate(
        from = interpolateFromY,
        to = 0f,
        duration = interpolateDuration,
        easing = Interpolate.Easing.EXP_OUT,
    )

    val interpolateScale = Interpolate(
        from = interpolateFromScale,
        to = 1f,
        duration = interpolateDuration,
        easing = Interpolate.Easing.EXP_OUT,
    )

    val initialScollX: Float = run {
        if (winner == null) return@run 0f
        val winnerIndex = views.indexOfFirst {
            it.teamKey == winner
        }
            .takeIf { it != -1 }
            ?: return@run 0f

        winnerIndex.toFloat() / (views.size - 1)
    }

    private var targetScrollX: Float = initialScollX
    private var scrollX: Float = initialScollX // 0-1
    private var preventMovementScrolling = false

    private val cardsAreaWidth get() = width * 6/10
    private val cardsTotalWidth get() =  views.size * ClientCardBufferRenderer.CARD_WIDTH

    private var dragStart: Pair<Double, Double>? = null
    private var dragInitialScrollX: Float = initialScollX
    private var isDraggingScrollBar: Boolean = false
    var isGui: Boolean = true

    fun viewsWithPositions(
        callback: (view: ClientCard, x: Float, y: Float, z: Int) -> Unit
    ) {
        if (views.isEmpty()) return

        val xMin = x - (cardsAreaWidth/2)
        val xMax = x + (cardsAreaWidth/2) - ClientCardBufferRenderer.CARD_WIDTH

        val widthDiff = (cardsTotalWidth - cardsAreaWidth).coerceAtLeast(0) / 2

        val scrollCenterX = (scrollX - 0.5f) * -2f
        val widthOffset = scrollCenterX * widthDiff

        val screenX = x - cardsTotalWidth/2
        val screenY = y - ClientCardBufferRenderer.CARD_HEIGHT /2

        val zOrderedViews = views.toList().withIndex()
            .sortedByDescending { (i) ->
                val xOffset = i * ClientCardBufferRenderer.CARD_WIDTH + widthOffset + (ClientCardBufferRenderer.CARD_WIDTH /2)
                val x = screenX + xOffset.roundToInt()
                val distanceToCenter = x - this.x
                distanceToCenter.absoluteValue
            }

        zOrderedViews.forEachIndexed { zIndex, (i, view) ->
            val xOffset = i * ClientCardBufferRenderer.CARD_WIDTH + widthOffset

            val x = screenX + xOffset
            val y = screenY
            val z = 302 * zIndex

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
                        interpolateX.get(),
                        interpolateY.get()
                    )
                }
                else -> Pair(xCoerced, y)
            }

            val view = if (isGui) view.guiCard else view
            callback(view, currentX, currentY, z)
        }
    }

    private fun getHoveredView(mouseX: Int, mouseY: Int): ClientCard? {
        var hoveredView: ClientCard? = null
        viewsWithPositions { view, x, y, z ->
            if (
                mouseX in x.toInt()..(x.toInt() + ClientCardBufferRenderer.CARD_WIDTH) &&
                mouseY in y.toInt()..(y.toInt() + ClientCardBufferRenderer.CARD_HEIGHT)
            ) {
                hoveredView = view
            }
        }

        return hoveredView
    }

    override fun render(drawService: IDrawService) {
        val mouse = drawService.mouse
        val mouseX = mouse.x
        val mouseY = mouse.y

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
            renderer.draw(
                drawService,
                view,
                x = x,
                y = y,
                z = z,
                cardScale = interpolateScale.get(),
                mouseX = mouseX,
                mouseY = mouseY,
                isFocused = isHovered && !isSingleView && onViewClick != null,
                isMouseOver = interpolateX.isDone() && isHovered,
                isWinner = winner != null && winner == view.teamKey,
            )
        }

        if (cardsTotalWidth > cardsAreaWidth) {
            val barX = x.toInt() - (cardsAreaWidth/2)
            val barY = y.toInt() + (height/2) + 3

            val barHeight = 2
            val barFilledWidth = cardsAreaWidth * cardsAreaWidth / cardsTotalWidth.coerceAtLeast(1)
            val barOffsetMax = cardsAreaWidth - barFilledWidth
            val barOffsetScrolled = (scrollX * barOffsetMax).roundToInt()

            val barColorBg = 0x80000000.toInt()
            val barColorFilled = when {
                dragStart == null && isInScrollBarBounds(mouseX, mouseY) -> 0xffb3b3c9.toInt()
                dragStart != null && isDraggingScrollBar -> 0xffffffff.toInt()
                else -> 0xff6b6b85.toInt()
            }

            drawService.fill(barX, barY, barX + barOffsetScrolled, barY + barHeight, barColorBg)
            drawService.fill(barX + barOffsetScrolled, barY, barX + barOffsetScrolled + barFilledWidth, barY + barHeight, barColorFilled)
            drawService.fill(barX + barOffsetScrolled + barFilledWidth, barY, barX + cardsAreaWidth, barY + barHeight, barColorBg)
        }
    }

    private fun isInBounds(mouseX: Int, mouseY: Int): Boolean {
        return mouseX in (x - width/2).toInt()..(x + width/2).toInt()
                && mouseY in (y - height/2).toInt()..(y + height/2).toInt()
    }

    private fun isInScrollBarBounds(mouseX: Int, mouseY: Int): Boolean {
        val barY = y.toInt() + (height/2) + 3
        val barHeight = 2
        val barPadding = 2
        return mouseX in (x - width/2).toInt()..(x + width/2).toInt()
                && mouseY in (barY - barPadding)..(barY + barHeight + barPadding)
    }

    fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (isInBounds(mouseX.toInt(), mouseY.toInt())) {
            if (amount.absoluteValue > 0.1) {
                preventMovementScrolling = true
            }

            targetScrollX = (targetScrollX + amount.toFloat() * -0.5f / views.size.coerceAtLeast(1)).coerceIn(0f, 1f)
            return true
        } else {
            return false
        }
    }

    fun onMouseDragged(mouseX: Double, mouseY: Double): Boolean {
        val initialMouseX: Double = dragStart?.first ?: mouseX

        if (dragStart == null) {
            isDraggingScrollBar = when {
                isInScrollBarBounds(mouseX.toInt(), mouseY.toInt()) -> true
                isInBounds(mouseX.toInt(), mouseY.toInt()) -> false
                else -> return false
            }
            dragStart = Pair(mouseX, mouseY)
            dragInitialScrollX = scrollX
        }

        if (isDraggingScrollBar) {
            val barFilledWidth = cardsAreaWidth * cardsAreaWidth / cardsTotalWidth.coerceAtLeast(1)
            (mouseX - initialMouseX).toFloat()
                .div((cardsAreaWidth - barFilledWidth).coerceAtLeast(1))
                .plus(dragInitialScrollX)
                .coerceIn(0f, 1f)
                .also { targetScrollX = it }
                .also { scrollX = it }
        } else {
            (initialMouseX - mouseX).toFloat()
                .div((cardsTotalWidth - cardsAreaWidth).coerceAtLeast(1))
                .plus(dragInitialScrollX)
                .coerceIn(0f, 1f)
                .also { targetScrollX = it }
        }

        return true
    }

    fun onMouseReleased(mouseX: Double, mouseY: Double): Boolean {
        return dragStart?.let {
            dragStart = null
            true
        } ?: false
    }

    fun onMouseClicked(mouseX: Double, mouseY: Double): Boolean {
        val hoveredView = getHoveredView(mouseX.toInt(), mouseY.toInt()) ?: return false

        var isHandled = false

        viewsWithPositions { view, x, y, _ ->
            if (hoveredView.teamKey != view.teamKey) return@viewsWithPositions
            val (tileX, tileY) = renderer.getItem(
                x = x.toInt(),
                y = y.toInt(),
                cardScale = interpolateScale.get(),
                mouseX = mouseX.toInt(),
                mouseY = mouseY.toInt(),
            ) ?: return@viewsWithPositions
            val tile = view.view.tile(tileX, tileY) ?: return@viewsWithPositions

            isHandled = onTileClick(tile)
        }

        if (!isHandled) {
            isHandled = onViewClick?.invoke(hoveredView) ?: false
        }

        return isHandled
    }

    fun onKeyPressed(input: IKeyInput): Boolean {
        val currentIndex = (scrollX * (views.size - 1)).roundToInt().coerceIn(views.indices)
        val newIndex = when {
            input.isArrowLeft -> currentIndex - 1
            input.isArrowRight -> currentIndex + 1
            else -> return false
        }

        targetScrollX = (newIndex.toFloat() / (views.size - 1)).coerceIn(0f, 1f)
        return true
    }
}