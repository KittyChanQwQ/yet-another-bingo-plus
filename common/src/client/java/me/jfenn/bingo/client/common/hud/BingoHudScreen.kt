package me.jfenn.bingo.client.common.hud

import me.jfenn.bingo.client.api.IButtonService
import me.jfenn.bingo.client.api.IDrawServiceFactory
import me.jfenn.bingo.client.integrations.jei.JeiIntegration
import me.jfenn.bingo.common.card.BingoTeamKey
import me.jfenn.bingo.common.map.CardView
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.koin.core.Koin
import kotlin.math.ceil

class BingoHudScreen(
    koin: Koin,
    text: TextProvider = koin.get(),
) : Screen(text.string(StringKey.CardTitle)) {

    private val drawServiceFactory = koin.get<IDrawServiceFactory>()
    private val buttonService = koin.get<IButtonService>()
    private val state = koin.get<BingoHudState>()
    private val jei = koin.get<JeiIntegration>()
    private val renderer = koin.get<BingoHudRenderer>()

    private fun viewsWithPositions(callback: (view: CardView, x: Int, y: Int) -> Unit) {
        val views = (state.display?.display ?: emptyList())
            .mapNotNull { state.views[it] }

        val rows = if (views.size > 3) 2 else 1
        val columns = ceil(views.size / rows.toFloat()).toInt()

        val screenX = width/2 - (BingoHudRenderer.CARD_WIDTH * columns)/2
        val screenY = height/2 - (BingoHudRenderer.CARD_HEIGHT * rows)/2

        for ((i, view) in views.withIndex()) {
            val col = i % columns
            val row = i / columns

            val x = screenX + (col * BingoHudRenderer.CARD_WIDTH)
            val y = screenY + (row * BingoHudRenderer.CARD_HEIGHT)
            callback(view, x, y)
        }
    }

    private var cardButtons = mapOf<BingoTeamKey?, ClickableWidget>()

    private fun updateButtons() {
        clearChildren()

        val newButtons = mutableMapOf<BingoTeamKey?, ClickableWidget>()

        viewsWithPositions { view, x, y ->
            // Don't create a button for the currently-selected card
            if (view.teamKey == state.selectedTeam)
                return@viewsWithPositions

            // Check if a button already exists for the view - otherwise, create one
            val cardName = view.teamColor?.name?.lowercase() ?: "preview"
            val button = cardButtons[view.teamKey] ?: buttonService.createButton(
                x, y, BingoHudRenderer.CARD_WIDTH, BingoHudRenderer.CARD_HEIGHT,
                texture = Identifier("minecraft", "bingo/card_$cardName"),
                focusedTexture = Identifier("minecraft", "bingo/card_$cardName"),
                onClick = {
                    state.selectedTeam = view.teamKey
                    close()
                },
                message = view.teamName,
            )

            button.x = x
            button.y = y
            newButtons[view.teamKey] = button
            addDrawableChild(button)
        }

        // If some views are no longer being displayed (e.g. a team is removed), remove them
        for (removedKey in cardButtons.keys - newButtons.keys) {
            cardButtons[removedKey]?.let { remove(it) }
        }

        cardButtons = newButtons
    }

    override fun init() {
        super.init()
        updateButtons()
    }

    override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        if (context == null) return
        try {
            renderBackground(context)
        } catch (e: NoSuchMethodError) {
            // This doesn't exist on 1.20.2+
        }

        super.render(context, mouseX, mouseY, delta)

        // Only call updateButtons() if the visible cards have changed
        val expectedButtons = state.display
            ?.display
            ?.filter { it != state.selectedTeam }
            ?.toSet()
            ?: emptySet()

        if (
            cardButtons.keys.size != expectedButtons.size ||
            !cardButtons.keys.containsAll(expectedButtons)
        ) {
            updateButtons()
        }

        val drawService = drawServiceFactory.create(context)

        viewsWithPositions { view, x, y ->
            val button = cardButtons[view.teamKey]
            renderer.drawCard(
                context,
                drawService,
                view,
                x = x,
                y = y,
                cardScale = 1f,
                mouseX = mouseX,
                mouseY = mouseY,
                showTeamOutlines = state.display?.display?.size?.let { it <= 1 } == true,
                isFocused = button?.isHovered == true || button?.isFocused == true,
            )
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        var ret = false

        viewsWithPositions { view, x, y ->
            val (tileX, tileY) = renderer.getItem(x, y, 1f, mouseX.toInt(), mouseY.toInt()) ?: return@viewsWithPositions
            val tile = view.tile(tileX, tileY) ?: return@viewsWithPositions
            val stack = tile.item ?: return@viewsWithPositions
            if (!tile.isAdvancement) {
                ret = jei.openJeiRecipe(stack.stack)
            }
        }

        return ret || super.mouseClicked(mouseX, mouseY, button)
    }

    override fun shouldPause() = false

}