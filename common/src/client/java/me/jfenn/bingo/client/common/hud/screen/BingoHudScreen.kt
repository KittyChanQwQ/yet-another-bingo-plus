package me.jfenn.bingo.client.common.hud.screen

import me.jfenn.bingo.client.common.BUTTON_GAME_CONTINUE
import me.jfenn.bingo.client.common.BUTTON_GAME_GENERIC
import me.jfenn.bingo.client.common.event.HudStateChangedEvent
import me.jfenn.bingo.client.common.hud.BingoEndFireworkRenderer
import me.jfenn.bingo.client.common.hud.card.ClientCardBufferRenderer
import me.jfenn.bingo.client.common.hud.card.ClientCardRenderer
import me.jfenn.bingo.client.common.packet.ClientPacketEvents
import me.jfenn.bingo.client.common.state.BingoHudState
import me.jfenn.bingo.client.common.state.ClientCard
import me.jfenn.bingo.client.common.utils.Interpolate
import me.jfenn.bingo.client.integrations.YetAnotherConfigLibIntegration
import me.jfenn.bingo.client.integrations.jei.IJeiApi
import me.jfenn.bingo.client.platform.IClient
import me.jfenn.bingo.client.platform.IScrollableWidgetFactory
import me.jfenn.bingo.client.platform.ITabsWidgetFactory
import me.jfenn.bingo.client.platform.renderer.IDrawService
import me.jfenn.bingo.client.platform.screen.*
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.game.GameOverPacket
import me.jfenn.bingo.common.map.CardTileAction
import me.jfenn.bingo.common.map.Color
import me.jfenn.bingo.common.ready.SetReadyPacket
import me.jfenn.bingo.common.ready.VoteOption
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.*
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.Identifier
import me.jfenn.bingo.platform.event.ICallbackHandle
import me.jfenn.bingo.platform.event.IEventBus
import org.joml.Vector2i
import org.koin.core.Koin
import kotlin.math.*

internal class BingoHudScreen(
    koin: Koin,
    private val gameOver: BingoHudState.GameOver?,
    private val canEscape: Boolean = false,
    private val text: TextProvider = koin.get(),
    private val packets: ClientPacketEvents = koin.get(),
    private val state: BingoHudState = koin.get(),
    private val client: IClient = koin.get(),
    private val config: BingoConfig = koin.get(),
    private val helper: IMutableScreenHelper,
    private val voteOptionsScreenFactory: BingoVoteOptionsScreen.Factory,
    originalCardKey: BingoTeamKey?,
    originalCardPos: Pair<Float, Float>,
    originalCardScale: Float = config.client.cardScale,
    scrollableWidgetFactory: IScrollableWidgetFactory = koin.get(),
    tabsWidgetFactory: ITabsWidgetFactory = koin.get(),
    buttonFactory: IButtonFactory = koin.get(),
    private val onCloseCallback: (BingoHudScreen) -> Unit,
    private val eventBus: IEventBus,
) : IScreen {

    private val jei by koin.inject<IJeiApi>()
    private val yacl by koin.inject<YetAnotherConfigLibIntegration>()

    class Factory(
        private val koin: Koin,
        private val text: TextProvider,
        private val packets: ClientPacketEvents,
        private val state: BingoHudState,
        private val config: BingoConfig,
        private val scrollableWidgetFactory: IScrollableWidgetFactory,
        private val tabsWidgetFactory: ITabsWidgetFactory,
        private val screenFactory: IScreenFactory,
        private val voteOptionsScreenFactory: BingoVoteOptionsScreen.Factory,
        private val eventBus: IEventBus,
    ) {
        fun create(
            gameOver: BingoHudState.GameOver?,
            originalCardKey: BingoTeamKey?,
            originalCardPos: Pair<Float, Float>,
            canEscape: Boolean,
            onClose: (BingoHudScreen) -> Unit,
        ) = screenFactory.build(text.string(StringKey.CardTitle)) { helper ->
            BingoHudScreen(
                koin = koin,
                gameOver = gameOver,
                canEscape = canEscape,
                text = text,
                packets = packets,
                state = state,
                config = config,
                helper = helper,
                voteOptionsScreenFactory = voteOptionsScreenFactory,
                originalCardKey = originalCardKey,
                originalCardPos = originalCardPos,
                scrollableWidgetFactory = scrollableWidgetFactory,
                tabsWidgetFactory = tabsWidgetFactory,
                onCloseCallback = onClose,
                eventBus = eventBus,
            )
        }
    }

    companion object {
        private val NEW_BEST_DURATION = 2.seconds
    }

    private val width by helper::width
    private val height by helper::height

    private val sidebarOffsetInterpolate = Interpolate(
        from = 0f,
        to = 1f,
        duration = 150.milliseconds,
    )
    private var sidebarWidth = 0

    private val cardsWidget = BingoCardsWidget(
        koin,

        interpolateKey = originalCardKey,
        interpolateFromX = originalCardPos.first,
        interpolateFromY = originalCardPos.second,
        interpolateFromScale = originalCardScale,
        interpolateDuration = 150.milliseconds,

        views = gameOver?.cards ?: state.cards.values,
        winner = gameOver?.packet?.winner,

        onTileClick = { tile ->
            if (gameOver != null) return@BingoCardsWidget false
            when (val action = tile.action) {
                is CardTileAction.Item -> jei.openItemRecipe(action.item)
                else -> false
            }
        },
        onViewClick = { view: ClientCard ->
            state.selectedTeam = view.teamKey
            helper.close()
            true
        }.takeIf { gameOver == null },
    )

    fun getCardPositions() = buildMap {
        // if the tab is not showing CARDS, don't interpolate the card positions
        if (tab != GameOverPacket.EndScreenTab.CARDS) return@buildMap
        cardsWidget.viewsWithPositions { view, x, y, _ ->
            put(view.teamKey, Pair(x, y))
        }
    }

    private val fireworkRenderer = BingoEndFireworkRenderer(text)
        .takeIf { gameOver != null && gameOver.isFirstOpen && gameOver.packet.isWinner }
        ?.also { gameOver?.isFirstOpen = false }

    private val settingsButton = buttonFactory.createNinePatchButton(
        sliceSize = 8,
        textureSize = Vector2i(200, 20),
        textures = BUTTON_GAME_GENERIC,
    ).also {
        it.message = text.string(StringKey.CardSettingsButton)
        it.onClick {
            client.screen = yacl.buildConfigScreen(helper.screen)
        }
    }.takeIf { yacl.isInstalled() }

    private val closeButton = buttonFactory.createNinePatchButton(
        sliceSize = 8,
        textureSize = Vector2i(200, 20),
        textures = BUTTON_GAME_GENERIC,
    ).also {
        it.message = text.string(StringKey.CardClose)
        it.onClick {
            helper.close()
        }
    }

    private val isMoreOptionsVisible get() = gameOver != null
            && packets.readySetV2.isSupported()
            && state.ready?.voteOptions.orEmpty().size > 1
    private val moreOptionsButton = buttonFactory.createNinePatchButton(
        sliceSize = 8,
        textureSize = Vector2i(200, 20),
        textures = BUTTON_GAME_GENERIC,
    ).also {
        it.message = text.string(StringKey.OptionsMore)
        it.onClick {
            client.screen = voteOptionsScreenFactory.create(gameOver, helper)
        }
    }

    private val isPrimaryButtonVisible get() = gameOver != null && (gameOver.packet.isReturnToLobbyAvailable || state.ready?.canSendReady == true)
    private var primaryVoteOption: VoteOption? = null
    private val primaryButton = buttonFactory.createNinePatchButton(
        sliceSize = 8,
        textureSize = Vector2i(200, 40),
        textures = BUTTON_GAME_CONTINUE,
    ).also { button ->
        button.message = text.string(StringKey.WorldReturnToLobby)
        button.onClick {
            sendReady(primaryVoteOption)
        }
    }

    private val tabsWidget = tabsWidgetFactory.create(GameOverPacket.EndScreenTab.entries.map { text.string(it.title) })
        .apply {
            currentTab = gameOver?.tab?.ordinal ?: 0
            onTabChanged { this@BingoHudScreen.onTabChanged() }
        }

    // If multiple teams have a duration (i.e. have completed the game), show the score list by default
    private val tab get() = GameOverPacket.EndScreenTab.entries.getOrNull(tabsWidget.currentTab)
    private val isShowingTabs get() = gameOver != null &&
            (
                    gameOver.packet.scores.size > 1 ||
                    tab != GameOverPacket.EndScreenTab.CARDS ||
                    gameOver.packet.defaultTab != GameOverPacket.EndScreenTab.CARDS
            )
    private val tabsHeight get() = tabsWidget.height / 2

    private fun onTabChanged() {
        init()
        tab?.let { gameOver?.tab = it }
    }

    private val scoresWidget = gameOver
        ?.let { scrollableWidgetFactory.create(BingoEndScoresWidget(koin, gameOver)) }

    private val messagesWidgetImpl = MessageHistoryWidget(koin, gameOver)
    private val messagesWidget = scrollableWidgetFactory.create(messagesWidgetImpl)

    private var sendingReadyPacket: Boolean = false
    private fun sendReady(voteOption: VoteOption?) {
        state.ready?.voteOptions?.forEach {
            it.clientSelected = it.id == voteOption?.id
        }

        val packet = SetReadyPacket(true, voteOption?.id)
        when {
            packets.readySetV2.send(packet) -> {}
            packets.readySetV1.send(packet) -> {}
        }
        // the game must be unpaused so that the server can process the ready packet
        sendingReadyPacket = true
    }

    private val sidebarDrawable = object : IDrawable {
        override fun render(drawService: IDrawService) {
            drawService.matrices.push()
            drawService.matrices.translate((1f-sidebarOffsetInterpolate.get(Interpolate.Easing.IN_OUT)) * sidebarWidth, 0f, 0f)

            // Ensure that the texture is rendered with an even width
            val evenOffset = if (sidebarWidth % 2 == 0) 0 else 1
            drawService.drawNinePatch(
                texture = Identifier.of("minecraft", "bingo/game_sidebar_background"),
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

    private fun updatePositions() {
        sidebarWidth = (width * 0.2f).toInt() + 100
        val interpolatedSidebarWidth = sidebarOffsetInterpolate.get(Interpolate.Easing.IN_OUT)
            .times(sidebarWidth)
            .roundToInt()

        tabsWidget.width = width - sidebarWidth

        cardsWidget.x = (width - sidebarWidth)/2f
        cardsWidget.y = height/2f + tabsHeight
        cardsWidget.width = width - sidebarWidth
        cardsWidget.height = ClientCardBufferRenderer.CARD_HEIGHT

        scoresWidget?.x = ((width - sidebarWidth) * 0.1f).toInt()
        scoresWidget?.y = (height/2) - (ClientCardBufferRenderer.CARD_HEIGHT /2) + tabsHeight
        scoresWidget?.width = ((width - sidebarWidth) * 0.8f).toInt()
        scoresWidget?.height = ClientCardBufferRenderer.CARD_HEIGHT

        val marginX = 12
        val marginY = 4

        val closeButtonSize = when {
            settingsButton != null -> (sidebarWidth - marginX * 2) / 2 - 1
            else -> sidebarWidth - marginX * 2
        }
        closeButton.size = Vector2i(closeButtonSize, 20)
        var buttonY = height - closeButton.size.y - 12
        closeButton.position = Vector2i(width - interpolatedSidebarWidth + marginX, buttonY)

        settingsButton?.size = Vector2i(closeButtonSize, 20)
        settingsButton?.position = Vector2i(width - interpolatedSidebarWidth + marginX + closeButtonSize + 2, buttonY)

        if (isMoreOptionsVisible) {
            moreOptionsButton.size = Vector2i(sidebarWidth - marginX * 2, 20)
            buttonY -= marginY + moreOptionsButton.size.y
            moreOptionsButton.position = Vector2i(width - interpolatedSidebarWidth + marginX, buttonY)
        }

        if (isPrimaryButtonVisible) {
            primaryButton.size = Vector2i(sidebarWidth - marginX * 2, 40)
            buttonY -= marginY + primaryButton.size.y
            primaryButton.position = Vector2i(width - interpolatedSidebarWidth + marginX, buttonY)
        }

        messagesWidget.x = width - interpolatedSidebarWidth + marginX
        messagesWidget.y = 0
        messagesWidget.width = sidebarWidth - marginX*2
        messagesWidget.height = buttonY - 10
    }

    override fun init() {
        helper.clearChildren()
        updatePositions()

        if (isShowingTabs) helper.addDrawableChild(tabsWidget.widget)

        if (tab == GameOverPacket.EndScreenTab.CARDS) {
            helper.addDrawable(cardsWidget)
        }
        if (scoresWidget != null && tab == GameOverPacket.EndScreenTab.SCORES) {
            helper.addDrawableChild(scoresWidget.widget!!)
        }

        helper.addDrawable(sidebarDrawable)

        messagesWidgetImpl.resize()
        helper.addDrawableChild(messagesWidget.widget!!)

        if (isPrimaryButtonVisible) {
            helper.addButton(primaryButton)
        }

        if (isMoreOptionsVisible) {
            helper.addButton(moreOptionsButton)
        }

        helper.addButton(closeButton)

        if (settingsButton != null) {
            helper.addButton(settingsButton)
        }
    }

    override fun resize(width: Int, height: Int) {
        init()
    }

    private fun renderGameDuration(drawService: IDrawService, titleY: Int, x: Float) {
        if (gameOver == null) return

        drawService.matrices.push()
        drawService.matrices.translate(x, titleY.toFloat(), 0f)
        drawService.matrices.rotate(-.25f)
        drawService.matrices.push()
        drawService.matrices.scale(2f, 2f, 1f)

        val gameDuration = text.literal(gameOver.packet.duration.formatString())
        drawService.drawText(
            gameDuration,
            -drawService.font.getTextWidth(gameDuration)/2, 0,
            0xFF_A3F5A3.toInt(),
            true,
        )

        drawService.matrices.pop()

        val title = text.string(StringKey.StatsGameDuration)
        drawService.drawText(title, -drawService.font.getTextWidth(title)/2, 18, 0xFF_FFFFFF.toInt(), true)

        gameOver.packet.prevBestTime?.let { prevBestTime ->
            drawService.matrices.push()

            if (gameOver.packet.isBestTime) {
                // Animate in a "New best time!" message
                val now = state.now
                val animTimeSince = now - gameOver.packet.endedAt
                val animProgress = (animTimeSince / NEW_BEST_DURATION).toFloat().coerceIn(0f, 1f).pow(2)
                val bestScale = 2f - animProgress
                val bestDiff = gameOver.packet.duration - prevBestTime
                val bestText = text.string(StringKey.StatsBestTimeNew, bestDiff.formatStringSmall())
                val bestColor = Color.fromInt(ClientCardRenderer.getWinColor(now))
                    .copy(a = (animProgress * 255).toInt().coerceAtLeast(6))
                    .asIntWithAlpha

                drawService.matrices.scale(bestScale, bestScale, 1f)
                drawService.drawText(bestText, -drawService.font.getTextWidth(bestText)/2, 30, bestColor, true)
            } else {
                val bestText = text.string(StringKey.StatsBestTime, prevBestTime.formatString())
                drawService.drawText(bestText, -drawService.font.getTextWidth(bestText)/2, 30, 0xFF_CCA3F5.toInt(), true)
            }

            drawService.matrices.pop()
        }

        drawService.matrices.pop()
    }

    private fun renderWinStreak(drawService: IDrawService, titleY: Int, x: Float) {
        val winStreak = gameOver?.packet?.winStreak ?: return
        val now = state.now

        drawService.matrices.push()
        drawService.matrices.translate(x, titleY.toFloat(), 0f)
        drawService.matrices.rotate(.25f)
        drawService.matrices.push()
        drawService.matrices.scale(2f, 2f, 1f)

        val winStreakText = text.literal("$winStreak")
        drawService.drawText(
            winStreakText,
            -drawService.font.getTextWidth(winStreakText)/2, 0,
            0xFF_F5A3D6.toInt(),
            true,
        )

        drawService.matrices.pop()

        val title = text.string(StringKey.StatsWinStreak)
        drawService.drawText(title, -drawService.font.getTextWidth(title)/2, 18, 0xFF_FFFFFF.toInt(), true)

        gameOver.packet.bestWinStreak?.let { bestWinStreak ->
            drawService.matrices.push()

            if (gameOver.packet.isBestWinStreak) {
                // Animate in a "New max streak!" message
                val animTimeSince = now - gameOver.packet.endedAt
                val animProgress = (animTimeSince / NEW_BEST_DURATION).toFloat().coerceIn(0f, 1f).pow(2)
                val bestScale = 2f - animProgress
                val bestText = text.string(StringKey.StatsWinStreakNewMax)
                val bestColor = Color.fromInt(ClientCardRenderer.getWinColor(now))
                    .copy(a = (animProgress * 255).toInt().coerceAtLeast(6))
                    .asIntWithAlpha

                drawService.matrices.scale(bestScale, bestScale, 1f)
                drawService.drawText(bestText, -drawService.font.getTextWidth(bestText)/2, 30, bestColor, true)
            } else {
                val bestText = text.string(StringKey.StatsWinStreakMax, bestWinStreak)
                drawService.drawText(bestText, -drawService.font.getTextWidth(bestText)/2, 30, 0xFF_CCA3F5.toInt(), true)
            }

            drawService.matrices.pop()
        }

        drawService.matrices.pop()

        if (winStreak > 1 && gameOver.packet.isWinner) {
            drawService.matrices.push()
            drawService.matrices.translate(x, titleY.toFloat() + 12, 0f)
            drawService.matrices.rotate(
                now.toEpochMilli()
                    .mod(2000)
                    .div(2000f)
                    .times(2 * PI.toFloat())
            )
            drawService.matrices.scale(8f, 8f, 1f)
            drawService.matrices.push()
            drawService.matrices.translate(-2f, -2f, 0f)

            drawService.fill(0, 0, 4, 4, 0x30_F5A3D6)

            drawService.matrices.pop()
            drawService.matrices.pop()
        }
    }

    override fun render(drawService: IDrawService, mouseX: Int, mouseY: Int, delta: Float) {
        // If the gameOver state is cleared, close the screen!
        if (gameOver != null && state.gameOver == null) {
            helper.close()
        }

        if (sidebarOffsetInterpolate.update()) {
            updatePositions()
        }

        val ready = state.ready
        primaryVoteOption = ready?.voteOptions?.find { it.id == ready.votePending }
        primaryVoteOption?.let {
            primaryButton.message = it.title
            primaryButton.active = !it.clientSelected
        } ?: run {
            primaryButton.active = ready?.isReady == false
        }

        state.ready?.takeIf { it.isRunning }?.subtitle?.let { title ->
            val titleWidth = drawService.font.getTextWidth(title)
            drawService.drawText(
                title,
                x = primaryButton.position.x + (primaryButton.size.x / 2) - (titleWidth / 2),
                y = primaryButton.position.y - 9,
                color = 0xff_888888.toInt(),
            )
        }

        val cardTop = (height/2) - (ClientCardBufferRenderer.CARD_HEIGHT /2)
        val titleY = min((cardTop * 0.75f).roundToInt(), cardTop - 11 * 2) + tabsHeight

        val decorationX = max((width - sidebarWidth) * 0.2f - 20f, 40f)
        renderGameDuration(drawService, titleY - 20, decorationX)
        renderWinStreak(drawService, titleY - 20, (width - sidebarWidth) - decorationX)

        if (tab == GameOverPacket.EndScreenTab.CARDS) {
            cardsWidget.views = gameOver?.cards ?: state.cards.values
            cardsWidget.winner = gameOver?.packet?.winner
        }

        fireworkRenderer?.render(drawService, (width - sidebarWidth)/2, height/2, state.now)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double): Boolean {
        return cardsWidget.onMouseDragged(mouseX, mouseY)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double): Boolean {
        return cardsWidget.onMouseReleased(mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return cardsWidget.onMouseClicked(mouseX, mouseY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        return cardsWidget.mouseScrolled(mouseX, mouseY, amount) || super.mouseScrolled(mouseX, mouseY, amount)
    }

    override fun keyPressed(input: IKeyInput): Boolean {
        return cardsWidget.onKeyPressed(input)
    }

    override fun shouldPause() = config.client.cardPausesGame && !sendingReadyPacket

    override fun shouldCloseOnEsc() = canEscape

    override fun onClose() {
        onCloseCallback.invoke(this)
    }

    private var hudStateListener: ICallbackHandle? = null

    override fun added() {
        hudStateListener = eventBus.register(HudStateChangedEvent) {
            init()
        }
    }

    override fun removed() {
        hudStateListener?.close()
        hudStateListener = null
    }
}