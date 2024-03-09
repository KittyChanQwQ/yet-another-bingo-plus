package me.jfenn.bingo.client.common.hud

import me.jfenn.bingo.client.api.IButtonService
import me.jfenn.bingo.client.api.IDrawServiceFactory
import me.jfenn.bingo.common.card.BingoTeamKey
import me.jfenn.bingo.common.map.CardView
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import kotlin.math.ceil

class BingoHudScreen(
    private val drawServiceFactory: IDrawServiceFactory,
    private val buttonService: IButtonService,
    private val state: BingoHudState,
) : Screen(Text.translatable("bingo.card.title")) {

    private val renderer = BingoHudRenderer()

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
        val newButtons = mutableMapOf<BingoTeamKey?, ClickableWidget>()

        viewsWithPositions { view, x, y ->
            // Don't create a button for the currently-selected card
            if (view.teamKey == state.selectedTeam)
                return@viewsWithPositions

            // Check if a button already exists for the view - otherwise, create one
            val button = cardButtons[view.teamKey] ?: buttonService.createButton(
                x, y, BingoHudRenderer.CARD_WIDTH, BingoHudRenderer.CARD_HEIGHT,
                texture = Identifier("minecraft", "bingo/hud_card"),
                focusedTexture = Identifier("minecraft", "bingo/hud_card_focused"),
                onClick = {
                    state.selectedTeam = view.teamKey
                    close()
                },
                message = Text.literal(view.teamName),
            ).also {
                addDrawableChild(it)
            }

            button.x = x
            button.y = y
            newButtons[view.teamKey] = button
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
        super.render(context, mouseX, mouseY, delta)
        if (context == null) return

        updateButtons()

        val drawService = drawServiceFactory.create(context)

        viewsWithPositions { view, x, y ->
            renderer.drawCard(context, drawService, view, x, y, 1f, mouseX, mouseY)
        }
    }

}