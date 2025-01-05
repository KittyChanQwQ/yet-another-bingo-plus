package me.jfenn.bingo.client.common.hud

import me.jfenn.bingo.client.common.utils.Interpolate
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
import kotlin.time.Duration.Companion.milliseconds

internal class BingoHudScreen(
    koin: Koin,
    private val config: BingoConfig,
    private val state: BingoHudState,
    private val jei: JeiIntegration,
    private val helper: IScreenHelper,
    originalCardPos: Pair<Float, Float>,
    originalCardScale: Float = config.client.cardScale,
    private val onCloseCallback: () -> Unit,
) : IScreen {

    class Factory(
        private val koin: Koin,
        private val config: BingoConfig,
        private val state: BingoHudState,
        private val jei: JeiIntegration,
        private val text: TextProvider,
        private val screenFactory: IScreenFactory,
    ) {
        fun create(
            originalCardPos: Pair<Float, Float>,
            onClose: () -> Unit,
        ) = screenFactory.build(text.string(StringKey.CardTitle)) { helper ->
            BingoHudScreen(
                koin = koin,
                config = config,
                state = state,
                jei = jei,
                helper = helper,
                originalCardPos = originalCardPos,
                onCloseCallback = onClose,
            )
        }
    }

    val width by helper::width
    val height by helper::height

    private val interpolateDuration = 150.milliseconds
    private val x = Interpolate(
        from = originalCardPos.first + BingoHudRenderer.CARD_WIDTH/2,
        to = width/2f,
        duration = interpolateDuration,
    )
    private val y = Interpolate(
        from = originalCardPos.first + BingoHudRenderer.CARD_HEIGHT/2,
        to = height/2f,
        duration = interpolateDuration,
    )
    private val scale = Interpolate(
        from = originalCardScale,
        to = 1f,
        duration = interpolateDuration,
    )

    private fun interpolate(interpolate: Interpolate): Float {
        // if there's >1 card view, this will not be a clean transition
        if (cardsWidget.views.size > 1) return interpolate.to
        return interpolate.get(Interpolate.Easing.IN_OUT)
    }

    private val cardsWidget = BingoCardsWidget(
        koin,

        x = x.get(Interpolate.Easing.IN_OUT),
        y = y.get(Interpolate.Easing.IN_OUT),
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
        x.to = width/2f
        y.to = height/2f

        cardsWidget.views = state.display?.display
            ?.mapNotNull { state.views[it] }
            ?: emptyList()
        cardsWidget.x = interpolate(x)
        cardsWidget.y = interpolate(y)
        cardsWidget.cardScale = interpolate(scale)
        // only enable tooltips once interpolation is over
        cardsWidget.isMouseOver = cardsWidget.views.size > 1 || x.isDone()
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

    override fun onClose() {
        onCloseCallback.invoke()
    }

}