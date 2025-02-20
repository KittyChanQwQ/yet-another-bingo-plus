package me.jfenn.bingo.client.common.hud

import me.jfenn.bingo.client.common.packet.ClientPacketEvents
import me.jfenn.bingo.client.platform.IScrollableWidgetFactory
import me.jfenn.bingo.client.platform.ITabsWidgetFactory
import me.jfenn.bingo.client.platform.renderer.IDrawService
import me.jfenn.bingo.client.platform.screen.IScreen
import me.jfenn.bingo.client.platform.screen.IScreenFactory
import me.jfenn.bingo.client.platform.screen.IScreenHelper
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.game.GameOverPacket.EndScreenTab
import me.jfenn.bingo.common.map.Color
import me.jfenn.bingo.common.ready.SetReadyPacket
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatHHMMSS
import me.jfenn.bingo.common.utils.formatString
import me.jfenn.bingo.common.utils.formatStringSmall
import me.jfenn.bingo.generated.StringKey
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.widget.ButtonWidget
import org.joml.AxisAngle4f
import org.joml.Quaternionf
import org.koin.core.Koin
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class BingoEndScreen(
    koin: Koin,
    private val gameOver: BingoHudState.GameOver,
    private val canEscape: Boolean = false,
    private val text: TextProvider = koin.get(),
    private val packets: ClientPacketEvents = koin.get(),
    private val state: BingoHudState = koin.get(),
    private val config: BingoConfig = koin.get(),
    private val helper: IScreenHelper,
    originalCardKey: BingoTeamKey?,
    originalCardPos: Pair<Float, Float>,
    originalCardScale: Float = config.client.cardScale,
    scrollableWidgetFactory: IScrollableWidgetFactory = koin.get(),
    tabsWidgetFactory: ITabsWidgetFactory = koin.get(),
    private val onCloseCallback: (BingoEndScreen) -> Unit,
) : IScreen {

    private val minecraftClient = MinecraftClient.getInstance()

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
        private const val BUTTON_WIDTH = 120
        private val NEW_BEST_DURATION = 2.seconds
    }

    private val width by helper::width
    private val height by helper::height
    private val client = MinecraftClient.getInstance()

    private val cardsWidget = BingoCardsWidget(
        koin,

        x = width/2f,
        y = height/2f,
        width = width * 6/10,
        height = BingoHudRenderer.CARD_HEIGHT,
        // If the game is paused, the client cannot receive updates for flashing tiles
        showFlashingTiles = !(minecraftClient.isInSingleplayer && shouldPause()),
        screen = helper.screen,

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
        if (tab != EndScreenTab.CARDS) return@buildMap
        cardsWidget.viewsWithPositions { view, x, y, z ->
            put(view.teamKey, Pair(x, y))
        }
    }

    private val fireworkRenderer = BingoEndFireworkRenderer(text)
        .takeIf { gameOver.isFirstOpen }
        ?.also { gameOver.isFirstOpen = false }

    private var dismissButton: ButtonWidget? = null
    private var restartButton: ButtonWidget? = null

    private val tabsWidget = tabsWidgetFactory.create(EndScreenTab.entries.map { text.string(it.title) })
        .apply {
            currentTab = EndScreenTab.entries.indexOf(gameOver.tab)
            onTabChanged { this@BingoEndScreen.onTabChanged() }
        }

    // If multiple teams have a duration (i.e. have completed the game), show the score list by default
    private val tab get() = EndScreenTab.entries.getOrNull(tabsWidget.currentTab)
    private val isShowingTabs get() = gameOver.packet.scores.size > 1 || tab != EndScreenTab.CARDS || gameOver.packet.defaultTab != EndScreenTab.CARDS
    private val tabsHeight get() = tabsWidget.height / 2

    private fun onTabChanged() {
        updateButtons()
        tab?.let { gameOver.tab = it }
    }

    private val scoresWidget = scrollableWidgetFactory.create(BingoEndScoresWidget(koin, gameOver))

    private fun reopenWorld() {
        val isInSingleplayer = client?.isInSingleplayer ?: return
        packets.readySetV1.send(SetReadyPacket(true))
        // If the screen is paused, it must be closed so that the server can process the ready packet
        if (isInSingleplayer && shouldPause()) {
            helper.close()
        }
    }

    private fun updateButtons() {
        helper.clearChildren()

        val dismissButton = dismissButton ?: ButtonWidget.builder(text.string(StringKey.CardClose).value) { helper.close() }
            .width(BUTTON_WIDTH)
            .position(0, 0)
            .build()
            .also {
                dismissButton = it
            }

        dismissButton.x = (width/2) - 4 - BUTTON_WIDTH
        dismissButton.y = (height/2) + (BingoHudRenderer.CARD_HEIGHT/2) + tabsHeight + 8
        helper.addDrawableChild(dismissButton)

        val restartButton = restartButton ?: ButtonWidget.builder(
            text.string(StringKey.WorldNextRound).value
        ) {
            it.active = false
            reopenWorld()
        }
            .width(BUTTON_WIDTH)
            .position(0, 0)
            .build()
            .also {
                restartButton = it
            }

        restartButton.x = (width/2) + 4
        restartButton.y = (height/2) + (BingoHudRenderer.CARD_HEIGHT/2) + tabsHeight + 8
        helper.addDrawableChild(restartButton)

        tabsWidget.width = width
        if (isShowingTabs) helper.addDrawableChild(tabsWidget.widget)

        if (tab == EndScreenTab.SCORES) {
            scoresWidget.x = (width * 0.1f).toInt()
            scoresWidget.y = (height/2) - (BingoHudRenderer.CARD_HEIGHT/2) + tabsHeight
            scoresWidget.width = (width * 0.8f).toInt()
            scoresWidget.height = BingoHudRenderer.CARD_HEIGHT
            helper.addDrawableChild(scoresWidget.widget!!)
        }
    }

    override fun init() {
        updateButtons()
    }

    override fun resize(width: Int, height: Int) {
        updateButtons()
    }

    private fun renderGameDuration(drawService: IDrawService, titleY: Int) {
        drawService.matrices.push()
        drawService.matrices.translate(width * 0.2f - 20f, titleY.toFloat(), 0f)
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
                val bestColor = Color.fromInt(BingoHudRenderer.getWinColor(now))
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

    private fun renderWinStreak(drawService: IDrawService, titleY: Int) {
        val winStreak = gameOver.packet.winStreak ?: return
        val now = state.now

        val ready = state.ready
        if (ready != null && ready.isRunning) {
            restartButton?.message = text.string(StringKey.LobbyNextRoundTimeRemaining, ready.remainingDuration.formatHHMMSS())
                .value
        } else {
            restartButton?.message = text.string(StringKey.WorldNextRound)
                .value
        }

        drawService.matrices.push()
        drawService.matrices.translate(width * 0.8f + 20f, titleY.toFloat(), 0f)
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
                val bestColor = Color.fromInt(BingoHudRenderer.getWinColor(now))
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
            drawService.matrices.translate(width * 0.8f + 20f, titleY.toFloat() + 12, 0f)
            drawService.matrices.multiply(Quaternionf(AxisAngle4f(
                now.toEpochMilliseconds()
                    .mod(2000)
                    .div(2000f)
                    .times(2*PI.toFloat()),
                0f, 0f, 1f
            )))
            drawService.matrices.scale(8f, 8f, 1f)
            drawService.matrices.push()
            drawService.matrices.translate(-2.5f, -3.5f, 0f)

            drawService.drawText(
                text.literal("◆"),
                0, 0,
                0x30_F5A3D6,
                false,
            )

            drawService.matrices.pop()
            drawService.matrices.pop()
        }
    }

    override fun render(drawService: IDrawService, mouseX: Int, mouseY: Int, delta: Float) {
        val cardTop = (height/2) - (BingoHudRenderer.CARD_HEIGHT/2)
        val titleY = min((cardTop * 0.75f).roundToInt(), cardTop - 11*2) + tabsHeight

        drawService.matrices.push()
        drawService.matrices.scale(2f, 2f, 2f)
        drawService.drawText(
            gameOver.packet.title,
            (width/4) - (drawService.font.getTextWidth(gameOver.packet.title)/2),
            (titleY/2) - 9,
            0xff_ffffff.toInt(),
            true,
        )
        drawService.matrices.pop()

        drawService.drawText(
            gameOver.packet.subtitle,
            (width/2) - (drawService.font.getTextWidth(gameOver.packet.subtitle)/2),
            titleY + 4,
            0xff_ffffff.toInt(),
            true,
        )

        renderGameDuration(drawService, titleY - 20)
        renderWinStreak(drawService, titleY - 20)

        if (tab == EndScreenTab.CARDS) {
            cardsWidget.views = gameOver.views
            cardsWidget.winner = gameOver.packet.winner
            cardsWidget.x = width/2f
            cardsWidget.y = height/2f + tabsHeight
            cardsWidget.width = width * 6/10
            cardsWidget.render(drawService, mouseX, mouseY, delta)
        }

        if (gameOver.packet.isWinner) {
            fireworkRenderer?.render(drawService, width/2, height/2, state.now)
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