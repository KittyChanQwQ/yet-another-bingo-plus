package me.jfenn.bingo.client.common.hud.screen

import me.jfenn.bingo.client.common.event.HudStateChangedEvent
import me.jfenn.bingo.client.common.hud.card.ClientCardBufferRenderer
import me.jfenn.bingo.client.common.state.BingoHudState
import me.jfenn.bingo.client.common.utils.Interpolate
import me.jfenn.bingo.client.integrations.JeiIntegration
import me.jfenn.bingo.client.integrations.jei.IJeiApi
import me.jfenn.bingo.client.platform.IScrollableWidgetFactory
import me.jfenn.bingo.client.platform.renderer.IDrawService
import me.jfenn.bingo.client.platform.screen.*
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.map.CardTileAction
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.event.IEventBus
import net.minecraft.util.Identifier
import org.joml.Vector2i
import org.koin.core.Koin
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

internal class BingoHudScreen(
    koin: Koin,
    private val config: BingoConfig,
    private val state: BingoHudState,
    private val jei: IJeiApi,
    buttonFactory: IButtonFactory,
    scrollableWidgetFactory: IScrollableWidgetFactory,
    text: TextProvider,
    private val helper: IMutableScreenHelper,
    originalCardKey: BingoTeamKey?,
    originalCardPos: Pair<Float, Float>,
    originalCardScale: Float = config.client.cardScale,
    private val onCloseCallback: (BingoHudScreen) -> Unit,
    eventBus: IEventBus,
) : IScreen {

    class Factory(
        private val koin: Koin,
        private val config: BingoConfig,
        private val state: BingoHudState,
        private val jei: JeiIntegration,
        private val text: TextProvider,
        private val screenFactory: IScreenFactory,
        private val buttonFactory: IButtonFactory,
        private val scrollableWidgetFactory: IScrollableWidgetFactory,
        private val eventBus: IEventBus,
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
                buttonFactory = buttonFactory,
                scrollableWidgetFactory = scrollableWidgetFactory,
                text = text,
                helper = helper,
                originalCardKey = originalCardKey,
                originalCardPos = originalCardPos,
                onCloseCallback = onClose,
                eventBus = eventBus,
            )
        }
    }

    val width by helper::width
    val height by helper::height

    private val sidebarOffsetInterpolate = Interpolate(
        from = 0f,
        to = 1f,
        duration = 150.milliseconds,
    )
    private var sidebarWidth = 0

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

    private val closeButton = buttonFactory.createNinePatchButton(
        sliceSize = 8,
        textureSize = Vector2i(200, 20),
        texture = "minecraft:bingo/button_game_generic",
        focusedTexture = "minecraft:bingo/button_game_generic_focused"
    ).also {
        it.message = text.string(StringKey.CardClose)
        it.onClick {
            helper.close()
        }
    }

    private val messagesWidgetImpl = MessageHistoryWidget(koin, null)
    private val messagesWidget = scrollableWidgetFactory.create(messagesWidgetImpl)

    private val sidebarDrawable = object : IDrawable {
        override fun render(drawService: IDrawService) {
            drawService.matrices.push()
            drawService.matrices.translate((1f-sidebarOffsetInterpolate.get(Interpolate.Easing.IN_OUT)) * sidebarWidth, 0f, 0f)

            // Ensure that the texture is rendered with an even width
            val evenOffset = if (sidebarWidth % 2 == 0) 0 else 1
            drawService.drawNinePatch(
                texture = Identifier.of("minecraft", "bingo/game_sidebar_background")!!,
                x = width - sidebarWidth - evenOffset,
                y = 0,
                width = sidebarWidth + evenOffset,
                height = height,
                sliceSize = 32,
                textureWidth = 128,
                textureHeight = 128,
            )

            drawService.matrices.pop()
        }
    }

    private val hudStateListener = eventBus.register(HudStateChangedEvent) {
        init()
    }

    fun getCardPositions() = buildMap {
        cardsWidget.viewsWithPositions { view, x, y, z ->
            put(view.teamKey, Pair(x, y))
        }
    }

    private fun updatePositions() {
        sidebarWidth = (width * 0.2f).toInt() + 100
        val interpolatedSidebarWidth = sidebarOffsetInterpolate.get(Interpolate.Easing.IN_OUT)
            .times(sidebarWidth)
            .roundToInt()

        cardsWidget.x = (width - sidebarWidth)/2f
        cardsWidget.y = height/2f
        cardsWidget.width = width - sidebarWidth
        cardsWidget.height = ClientCardBufferRenderer.CARD_HEIGHT

        val marginX = 12

        closeButton.size = Vector2i(sidebarWidth - marginX * 2, 20)
        val buttonY = height - closeButton.size.y - 12
        closeButton.position = Vector2i(width - interpolatedSidebarWidth + marginX, buttonY)

        messagesWidget.x = width - interpolatedSidebarWidth + marginX
        messagesWidget.y = 0
        messagesWidget.width = sidebarWidth - marginX*2
        messagesWidget.height = buttonY
    }

    override fun init() {
        helper.clearChildren()
        updatePositions()

        helper.addDrawable(cardsWidget)

        helper.addDrawable(sidebarDrawable)

        helper.addButton(closeButton)

        messagesWidgetImpl.resize()
        helper.addDrawableChild(messagesWidget.widget!!)
    }

    override fun resize(width: Int, height: Int) {
        init()
    }

    override fun render(drawService: IDrawService, mouseX: Int, mouseY: Int, delta: Float) {
        if (state.now - sidebarOffsetInterpolate.startedAt < sidebarOffsetInterpolate.duration + 50.milliseconds) {
            updatePositions()
        }

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
        hudStateListener.close()
    }

}