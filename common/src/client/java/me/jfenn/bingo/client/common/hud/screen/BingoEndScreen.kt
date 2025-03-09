package me.jfenn.bingo.client.common.hud.screen

import me.jfenn.bingo.client.common.hud.BingoEndFireworkRenderer
import me.jfenn.bingo.client.common.hud.card.ClientCardBufferRenderer
import me.jfenn.bingo.client.common.hud.card.ClientCardRenderer
import me.jfenn.bingo.client.common.packet.ClientPacketEvents
import me.jfenn.bingo.client.common.state.BingoHudState
import me.jfenn.bingo.client.common.utils.Interpolate
import me.jfenn.bingo.client.platform.IClient
import me.jfenn.bingo.client.platform.IScrollableWidgetFactory
import me.jfenn.bingo.client.platform.ITabsWidgetFactory
import me.jfenn.bingo.client.platform.renderer.IDrawService
import me.jfenn.bingo.client.platform.screen.*
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.game.GameCommands
import me.jfenn.bingo.common.game.GameOverPacket
import me.jfenn.bingo.common.map.Color
import me.jfenn.bingo.common.ready.SetReadyPacket
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatHHMMSS
import me.jfenn.bingo.common.utils.formatString
import me.jfenn.bingo.common.utils.formatStringSmall
import me.jfenn.bingo.generated.StringKey
import net.minecraft.util.Identifier
import org.joml.AxisAngle4f
import org.joml.Quaternionf
import org.joml.Vector2i
import org.koin.core.Koin
import kotlin.math.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class BingoEndScreen(
    koin: Koin,
    private val gameOver: BingoHudState.GameOver,
    private val canEscape: Boolean = false,
    private val text: TextProvider = koin.get(),
    private val packets: ClientPacketEvents = koin.get(),
    private val state: BingoHudState = koin.get(),
    private val client: IClient = koin.get(),
    private val config: BingoConfig = koin.get(),
    private val helper: IMutableScreenHelper,
    originalCardKey: BingoTeamKey?,
    originalCardPos: Pair<Float, Float>,
    originalCardScale: Float = config.client.cardScale,
    scrollableWidgetFactory: IScrollableWidgetFactory = koin.get(),
    tabsWidgetFactory: ITabsWidgetFactory = koin.get(),
    buttonFactory: IButtonFactory = koin.get(),
    private val onCloseCallback: (BingoEndScreen) -> Unit,
) : IScreen {

    class Factory(
        private val koin: Koin,
        private val text: TextProvider = koin.get(),
        private val packets: ClientPacketEvents = koin.get(),
        private val state: BingoHudState = koin.get(),
        private val config: BingoConfig = koin.get(),
        private val scrollableWidgetFactory: IScrollableWidgetFactory,
        private val tabsWidgetFactory: ITabsWidgetFactory,
        private val screenFactory: IScreenFactory,
    ) {
        fun create(
            gameOver: BingoHudState.GameOver,
            originalCardKey: BingoTeamKey?,
            originalCardPos: Pair<Float, Float>,
            canEscape: Boolean,
            onClose: (BingoEndScreen) -> Unit,
        ) = screenFactory.build(text.string(StringKey.CardTitle)) { helper ->
            BingoEndScreen(
                koin = koin,
                gameOver = gameOver,
                canEscape = canEscape,
                text = text,
                packets = packets,
                state = state,
                config = config,
                helper = helper,
                originalCardKey = originalCardKey,
                originalCardPos = originalCardPos,
                scrollableWidgetFactory = scrollableWidgetFactory,
                tabsWidgetFactory = tabsWidgetFactory,
                onCloseCallback = onClose,
            )
        }
    }

    companion object {
        private val NEW_BEST_DURATION = 2.seconds
    }

    private val width by helper::width
    private val height by helper::height

    private val sidebarOffsetInterpolate = Interpolate(
        from = 1f,
        to = 0f,
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

        onTileClick = { false },
        onViewClick = null,
    )

    fun getCardPositions() = buildMap {
        // if the tab is not showing CARDS, don't interpolate the card positions
        if (tab != GameOverPacket.EndScreenTab.CARDS) return@buildMap
        cardsWidget.viewsWithPositions { view, x, y, z ->
            put(view.teamKey, Pair(x, y))
        }
    }

    private val fireworkRenderer = BingoEndFireworkRenderer(text)
        .takeIf { gameOver.isFirstOpen }
        ?.also { gameOver.isFirstOpen = false }

    private val closeButton = buttonFactory.createNinePatchButton(
        sliceSize = 8,
        textureSize = Vector2i(200, 20),
        texture = "minecraft:bingo/button_game_generic",
        focusedTexture = "minecraft:bingo/button_game_generic_focused"
    ).also {
        it.onClick {
            helper.close()
        }
    }

    private val keepPlayingButton = buttonFactory.createNinePatchButton(
        sliceSize = 8,
        textureSize = Vector2i(200, 20),
        texture = "minecraft:bingo/button_game_generic",
        focusedTexture = "minecraft:bingo/button_game_generic_focused"
    ).also {
        it.onClick {
            client.player.sendCommand(GameCommands.RESUME_COMMAND)
            helper.close()
        }
    }

    private val primaryButton = buttonFactory.createNinePatchButton(
        sliceSize = 8,
        textureSize = Vector2i(200, 40),
        texture = "minecraft:bingo/button_game_continue",
        focusedTexture = "minecraft:bingo/button_game_continue_focused"
    ).also {
        it.onClick {
            sendReadyForNextRound()
        }
    }

    private val tabsWidget = tabsWidgetFactory.create(GameOverPacket.EndScreenTab.entries.map { text.string(it.title) })
        .apply {
            currentTab = GameOverPacket.EndScreenTab.entries.indexOf(gameOver.tab)
            onTabChanged { this@BingoEndScreen.onTabChanged() }
        }

    // If multiple teams have a duration (i.e. have completed the game), show the score list by default
    private val tab get() = GameOverPacket.EndScreenTab.entries.getOrNull(tabsWidget.currentTab)
    private val isShowingTabs get() = gameOver.packet.scores.size > 1 || tab != GameOverPacket.EndScreenTab.CARDS || gameOver.packet.defaultTab != GameOverPacket.EndScreenTab.CARDS
    private val tabsHeight get() = tabsWidget.height / 2

    private fun onTabChanged() {
        updateButtons()
        tab?.let { gameOver.tab = it }
    }

    private val scoresWidget = scrollableWidgetFactory.create(BingoEndScoresWidget(koin, gameOver))

    private val messagesWidgetImpl = MessageHistoryWidget(koin, gameOver)
    private val messagesWidget = scrollableWidgetFactory.create(messagesWidgetImpl)

    private fun sendReadyForNextRound() {
        packets.readySetV1.send(SetReadyPacket(true))
        // If the screen is paused, it must be closed so that the server can process the ready packet
        if (client.isInSingleplayer && shouldPause()) {
            helper.close()
        }
    }

    private fun updateButtons() {
        helper.clearChildren()

        sidebarWidth = (width * 0.2f).toInt() + 100

        tabsWidget.width = width - sidebarWidth
        if (isShowingTabs) helper.addDrawableChild(tabsWidget.widget)

        if (tab == GameOverPacket.EndScreenTab.CARDS) {
            cardsWidget.x = (width - sidebarWidth)/2f
            cardsWidget.y = height/2f + tabsHeight
            cardsWidget.width = width - sidebarWidth
            cardsWidget.height = ClientCardBufferRenderer.CARD_HEIGHT
            helper.addDrawable(cardsWidget)
        }
        if (tab == GameOverPacket.EndScreenTab.SCORES) {
            scoresWidget.x = ((width - sidebarWidth) * 0.1f).toInt()
            scoresWidget.y = (height/2) - (ClientCardBufferRenderer.CARD_HEIGHT /2) + tabsHeight
            scoresWidget.width = ((width - sidebarWidth) * 0.8f).toInt()
            scoresWidget.height = ClientCardBufferRenderer.CARD_HEIGHT
            helper.addDrawableChild(scoresWidget.widget!!)
        }

        helper.addDrawable(
            object : IDrawable {
                override fun render(drawService: IDrawService) {
                    drawService.matrices.push()
                    drawService.matrices.translate(sidebarOffsetInterpolate.get(Interpolate.Easing.IN_OUT) * sidebarWidth, 0f, 0f)
                    renderSidebar(drawService)
                    drawService.matrices.pop()
                }
            }
        )

        val marginX = 12
        val marginY = 4

        closeButton.size = Vector2i(sidebarWidth - marginX * 2, 20)
        var buttonY = height - closeButton.size.y - 12
        closeButton.position = Vector2i(width - sidebarWidth + marginX, buttonY)
        closeButton.message = text.string(StringKey.CardClose)
        helper.addButton(closeButton)

        if (gameOver.packet.isResumeAvailable) {
            keepPlayingButton.size = Vector2i(sidebarWidth - marginX * 2, 20)
            buttonY -= marginY + keepPlayingButton.size.y
            keepPlayingButton.position = Vector2i(width - sidebarWidth + marginX, buttonY)
            keepPlayingButton.message = text.string(StringKey.WorldKeepPlaying)
            helper.addButton(keepPlayingButton)
        }

        if (gameOver.packet.isReturnToLobbyAvailable) {
            primaryButton.size = Vector2i(sidebarWidth - marginX * 2, 40)
            buttonY -= marginY + primaryButton.size.y
            primaryButton.position = Vector2i(width - sidebarWidth + marginX, buttonY)
            primaryButton.message = text.string(StringKey.WorldReturnToLobby)
            helper.addButton(primaryButton)
        }

        messagesWidget.x = width - sidebarWidth + marginX
        messagesWidget.y = 0
        messagesWidget.width = sidebarWidth - marginX*2
        messagesWidget.height = buttonY
        messagesWidgetImpl.resize()
        helper.addDrawableChild(messagesWidget.widget!!)
    }

    override fun init() {
        updateButtons()
    }

    override fun resize(width: Int, height: Int) {
        updateButtons()
    }

    private fun renderGameDuration(drawService: IDrawService, titleY: Int, x: Float) {
        drawService.matrices.push()
        drawService.matrices.translate(x, titleY.toFloat(), 0f)
        drawService.matrices.multiply(Quaternionf(AxisAngle4f(-.25f, 0f, 0f, 1f)))
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
        val winStreak = gameOver.packet.winStreak ?: return
        val now = state.now

        val ready = state.ready
        if (ready != null && ready.isRunning) {
            primaryButton.message = text.string(StringKey.LobbyNextRoundTimeRemaining, ready.remainingDuration.formatHHMMSS())
        } else {
            primaryButton.message = text.string(StringKey.WorldReturnToLobby)
        }

        drawService.matrices.push()
        drawService.matrices.translate(x, titleY.toFloat(), 0f)
        drawService.matrices.multiply(Quaternionf(AxisAngle4f(.25f, 0f, 0f, 1f)))
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
            drawService.matrices.multiply(
                Quaternionf(
                    AxisAngle4f(
                        now.toEpochMilliseconds()
                            .mod(2000)
                            .div(2000f)
                            .times(2 * PI.toFloat()),
                        0f, 0f, 1f
                    )
                )
            )
            drawService.matrices.scale(8f, 8f, 1f)
            drawService.matrices.push()
            drawService.matrices.translate(-3f, -3f, 0f)

            drawService.context.fill(0, 0, 6, 6, 0x30_F5A3D6)

            drawService.matrices.pop()
            drawService.matrices.pop()
        }
    }

    private fun renderSidebar(drawService: IDrawService) {
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
    }

    override fun render(drawService: IDrawService, mouseX: Int, mouseY: Int, delta: Float) {
        val cardTop = (height/2) - (ClientCardBufferRenderer.CARD_HEIGHT /2)
        val titleY = min((cardTop * 0.75f).roundToInt(), cardTop - 11 * 2) + tabsHeight

        val decorationX = max((width - sidebarWidth) * 0.2f - 20f, 40f)
        renderGameDuration(drawService, titleY - 20, decorationX)
        renderWinStreak(drawService, titleY - 20, (width - sidebarWidth) - decorationX)

        if (tab == GameOverPacket.EndScreenTab.CARDS) {
            cardsWidget.views = gameOver.cards
            cardsWidget.winner = gameOver.packet.winner
        }

        if (gameOver.packet.isWinner) {
            fireworkRenderer?.render(drawService, (width - sidebarWidth)/2, height/2, state.now)
        }
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        return cardsWidget.mouseScrolled(mouseX, mouseY, amount) || super.mouseScrolled(mouseX, mouseY, amount)
    }

    override fun shouldPause() = config.client.cardPausesGame

    override fun shouldCloseOnEsc() = canEscape

    override fun onClose() {
        onCloseCallback.invoke(this)
    }

}