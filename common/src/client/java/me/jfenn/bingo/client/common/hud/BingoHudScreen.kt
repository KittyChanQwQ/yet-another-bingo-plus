package me.jfenn.bingo.client.common.hud

import me.jfenn.bingo.client.integrations.jei.JeiIntegration
import me.jfenn.bingo.client.platform.screen.IScreen
import me.jfenn.bingo.client.platform.screen.IScreenFactory
import me.jfenn.bingo.client.platform.screen.IScreenHelper
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.map.CardTile
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import org.koin.core.Koin

internal class BingoHudScreen(
    koin: Koin,
    private val config: BingoConfig,
    private val state: BingoHudState,
    private val jei: JeiIntegration,
    private val helper: IScreenHelper,
) : IScreen {

    class Factory(
        private val koin: Koin,
        private val config: BingoConfig,
        private val state: BingoHudState,
        private val jei: JeiIntegration,
        private val text: TextProvider,
        private val screenFactory: IScreenFactory,
    ) {
        fun create() = screenFactory.build(text.string(StringKey.CardTitle)) { helper ->
            BingoHudScreen(
                koin = koin,
                config = config,
                state = state,
                jei = jei,
                helper = helper,
            )
        }
    }

    val width by helper::width
    val height by helper::height

    private val cardsWidget = BingoCardsWidget(
        koin,

        x = width/2,
        y = height/2,
        width = width * 6/10,
        height = BingoHudRenderer.CARD_HEIGHT,
        // If the game is paused, the client cannot receive updates for flashing tiles
        showFlashingTiles = !(MinecraftClient.getInstance().isInSingleplayer && shouldPause()),
        screen = helper.screen,

        onTileClick = { tile ->
            val stack = tile.image.item
            if ((tile.decoration == null || tile.decoration == CardTile.Decoration.NONE) && stack != null) {
                jei.openJeiRecipe(stack.stack)
            } else {
                false
            }
        },
        onViewClick = { view ->
            state.selectedTeam = view.teamKey
            helper.close()
            true
        },
    )

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        cardsWidget.views = state.display?.display
            ?.mapNotNull { state.views[it] }
            ?: emptyList()
        cardsWidget.x = width/2
        cardsWidget.y = height/2
        cardsWidget.width = width * 6/10
        cardsWidget.render(context, mouseX, mouseY, delta)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return cardsWidget.onMouseClicked(mouseX, mouseY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        return cardsWidget.mouseScrolled(mouseX, mouseY, amount)
    }

    override fun shouldPause() = config.client.cardPausesGame

}