package me.jfenn.bingo.client.common.hud

import me.jfenn.bingo.client.integrations.jei.JeiIntegration
import me.jfenn.bingo.common.map.CardTile
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import org.koin.core.Koin

class BingoHudScreen(
    koin: Koin,
    text: TextProvider = koin.get(),
) : Screen(text.string(StringKey.CardTitle)) {

    private val state = koin.get<BingoHudState>()
    private val jei = koin.get<JeiIntegration>()

    private val cardsWidget = BingoCardsWidget(
        koin,

        x = width/2,
        y = height/2,
        width = width * 6/10,
        height = BingoHudRenderer.CARD_HEIGHT,
        screen = this,

        onTileClick = { tile ->
            val stack = tile.item
            if ((tile.decoration == null || tile.decoration == CardTile.Decoration.NONE) && stack != null) {
                jei.openJeiRecipe(stack.stack)
            }
            jei.isInstalled()
        },
        onViewClick = { view ->
            state.selectedTeam = view.teamKey
            this.close()
            true
        },
    )

    override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        if (context == null) return
        try {
            renderBackground(context)
        } catch (e: NoSuchMethodError) {
            // This doesn't exist on 1.20.2+
        }

        super.render(context, mouseX, mouseY, delta)

        cardsWidget.views = state.display?.display
            ?.mapNotNull { state.views[it] }
            ?: emptyList()
        cardsWidget.x = width/2
        cardsWidget.y = height/2
        cardsWidget.width = width * 6/10
        cardsWidget.render(context, mouseX, mouseY, delta)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return cardsWidget.onMouseClicked(mouseX, mouseY) || super.mouseClicked(mouseX, mouseY, button)
    }

    override fun shouldPause() = false

}