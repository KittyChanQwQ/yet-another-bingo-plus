package me.jfenn.bingo.client.common.hud

import me.jfenn.bingo.client.api.IDrawServiceFactory
import me.jfenn.bingo.common.map.CardTile
import me.jfenn.bingo.common.map.CardView
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import org.koin.core.Koin
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class BingoCardsWidget(
    koin: Koin,

    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int,

    val onTileClick: (CardTile) -> Boolean,
    val onViewClick: ((CardView) -> Boolean)?,

    private val screen: Screen,
    private val drawServiceFactory: IDrawServiceFactory = koin.get(),
    private val state: BingoHudState = koin.get(),
    private val renderer: BingoHudRenderer = koin.get()
) {

    private var targetScrollX: Float = 0f
    private var scrollX: Float = 0f // 0-1

    private val cardsAreaMargin get() = screen.width * 2/10
    private val cardsAreaWidth get() = screen.width - cardsAreaMargin*2

    private fun viewsWithPositions(
        callback: (view: CardView, x: Int, y: Int, z: Int) -> Unit
    ) {
        val views = (state.display?.display ?: emptyList())
            .mapNotNull { state.views[it] }

        if (views.isEmpty()) return

        val xMin = x - (width/2)
        val xMax = x + (width/2) - BingoHudRenderer.CARD_WIDTH

        val widthTotal = views.size * BingoHudRenderer.CARD_WIDTH
        val widthDiff = (widthTotal - cardsAreaWidth).coerceAtLeast(0) / 2

        val scrollCenterX = (scrollX - 0.5f) * -2f
        val widthOffset = scrollCenterX * widthDiff

        val screenX = x - widthTotal/2
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

            val x = screenX + xOffset.roundToInt()
            val y = screenY

            val zIndex = zOrder.indexOf(i)
            val z = 201 * (views.size - zIndex)

            val xCoerced = when {
                x < xMin -> x.coerceAtLeast(xMin - 4 * (views.size - i - 1))
                x > xMax -> x.coerceAtMost(xMax + 4 * i)
                else -> x
            }

            callback(view, xCoerced, y, z)
        }
    }

    private fun getHoveredView(mouseX: Int, mouseY: Int): CardView? {
        var hoveredView: CardView? = null
        var hoveredZ = 0
        viewsWithPositions { view, x, y, z ->
            if (
                mouseX in x..(x + BingoHudRenderer.CARD_WIDTH) &&
                mouseY in y..(y + BingoHudRenderer.CARD_HEIGHT) &&
                z > hoveredZ
            ) {
                hoveredView = view
                hoveredZ = z
            }
        }

        return hoveredView
    }

    fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        if (isInBounds(x, mouseY)) {
            val boundaryMin = x - width*.5f
            val boundaryMax = x + width*.5f
            val baseVelocity = 0.01f / (1 + (state.display?.display?.size ?: 0))
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

        val drawService = drawServiceFactory.create(context)

        val hoveredView = getHoveredView(mouseX, mouseY)

        viewsWithPositions { view, x, y, z ->
            val isHovered = hoveredView != null && hoveredView.teamKey == view.teamKey
            val isSingleView = state.display?.display?.size?.let { it <= 1 } == true
            renderer.drawCard(
                context,
                drawService,
                view,
                x = x,
                y = y,
                z = z,
                cardScale = 1f,
                mouseX = mouseX,
                mouseY = mouseY,
                showTeamOutlines = isSingleView,
                isFocused = isHovered && !isSingleView && onViewClick != null,
                isMouseOver = isHovered,
            )
        }
    }

    private fun isInBounds(mouseX: Int, mouseY: Int): Boolean {
        return mouseX in (x - width/2)..(x + width/2)
                && mouseY in (y - height/2)..(y + height/2)
    }

    fun onMouseClicked(mouseX: Double, mouseY: Double): Boolean {
        val hoveredView = getHoveredView(mouseX.toInt(), mouseY.toInt()) ?: return false

        var isHandled = false

        viewsWithPositions { view, x, y, _ ->
            if (hoveredView.teamKey != view.teamKey) return@viewsWithPositions
            val (tileX, tileY) = renderer.getItem(x, y, 1f, mouseX.toInt(), mouseY.toInt()) ?: return@viewsWithPositions
            val tile = view.tile(tileX, tileY) ?: return@viewsWithPositions

            isHandled = onTileClick(tile)
        }

        if (!isHandled) {
            isHandled = onViewClick?.invoke(hoveredView) ?: false
        }

        return isHandled
    }

}