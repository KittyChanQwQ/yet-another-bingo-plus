package me.jfenn.bingo.client.common.hud.screen

import me.jfenn.bingo.client.common.hud.card.ClientCardBufferRenderer
import me.jfenn.bingo.client.common.state.BingoHudState
import me.jfenn.bingo.client.integrations.JeiIntegration
import me.jfenn.bingo.client.integrations.jei.IJeiApi
import me.jfenn.bingo.client.platform.renderer.IDrawService
import me.jfenn.bingo.client.platform.screen.IMutableScreenHelper
import me.jfenn.bingo.client.platform.screen.IScreen
import me.jfenn.bingo.client.platform.screen.IScreenFactory
import me.jfenn.bingo.client.platform.screen.IScreenHelper
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.map.CardTileAction
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import org.koin.core.Koin
import kotlin.time.Duration.Companion.milliseconds

internal class BingoHudScreen(
    koin: Koin,
    private val config: BingoConfig,
    private val state: BingoHudState,
    private val jei: IJeiApi,
    private val helper: IMutableScreenHelper,
    originalCardKey: BingoTeamKey?,
    originalCardPos: Pair<Float, Float>,
    originalCardScale: Float = config.client.cardScale,
    private val onCloseCallback: (BingoHudScreen) -> Unit,
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
            originalCardKey: BingoTeamKey?,
            originalCardPos: Pair<Float, Float>,
            onClose: (BingoHudScreen) -> Unit,
        ) = screenFactory.build(text.string(StringKey.CardTitle)) { helper ->
            BingoHudScreen(
                koin = koin,
                config = config,
                state = state,
                jei = jei,
                helper = helper,
                originalCardKey = originalCardKey,
                originalCardPos = originalCardPos,
                onCloseCallback = onClose,
            )
        }
    }

    val width by helper::width
    val height by helper::height

    private val cardsWidget = BingoCardsWidget(
        koin,

        x = width / 2f,
        y = height / 2f,
        width = width,
        height = ClientCardBufferRenderer.CARD_HEIGHT,

        interpolateKey = originalCardKey,
        interpolateFromX = originalCardPos.first,
        interpolateFromY = originalCardPos.second,
        interpolateFromScale = originalCardScale,
        interpolateDuration = 150.milliseconds,

        onTileClick = { tile ->
            when (val action = tile.action) {
                is CardTileAction.Item -> jei.openItemRecipe(action.item.stack)
                else -> false
            }
        },
        onViewClick = { view ->
            state.selectedTeam = view.teamKey
            helper.close()
            true
        },
    )

    fun getCardPositions() = buildMap {
        cardsWidget.viewsWithPositions { view, x, y, z ->
            put(view.teamKey, Pair(x, y))
        }
    }

    override fun init() {
        helper.clearChildren()

        cardsWidget.x = width/2f
        cardsWidget.y = height/2f
        cardsWidget.width = width

        helper.addDrawable(cardsWidget)
    }

    override fun resize(width: Int, height: Int) {
        init()
    }

    override fun render(drawService: IDrawService, mouseX: Int, mouseY: Int, delta: Float) {
        cardsWidget.views = state.cards.values
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return cardsWidget.onMouseClicked(mouseX, mouseY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        return cardsWidget.mouseScrolled(mouseX, mouseY, amount)
    }

    override fun shouldPause() = config.client.cardPausesGame

    override fun onClose() {
        onCloseCallback.invoke(this)
    }

}