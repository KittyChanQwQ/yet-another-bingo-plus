package me.jfenn.bingo.client.common.hud

import kotlinx.datetime.Clock
import me.jfenn.bingo.client.api.IWorldService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatString
import me.jfenn.bingo.generated.StringKey
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import org.joml.AxisAngle4f
import org.joml.Quaternionf
import org.koin.core.Koin
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt

class BingoEndScreen(
    koin: Koin,
    private val gameOver: BingoHudState.GameOver,
    private val canEscape: Boolean = false,
    private val text: TextProvider = koin.get(),
) : Screen(text.string(StringKey.CardTitle)) {

    private val worldService = koin.get<IWorldService>()

    companion object {
        private const val BUTTON_WIDTH = 120
    }

    private val cardsWidget = BingoCardsWidget(
        koin,

        x = width/2,
        y = height/2,
        width = width * 6/10,
        height = BingoHudRenderer.CARD_HEIGHT,
        screen = this,

        onTileClick = { false },
        onViewClick = null,
    )

    private val fireworkRenderer = BingoEndFireworkRenderer()

    private var dismissButton: ButtonWidget? = null
    private var restartButton: ButtonWidget? = null

    private fun reopenWorld() {
        val client = client ?: return
        when {
            client.isInSingleplayer -> worldService.disconnectAndReopen()
            gameOver.packet.canSendRestart -> worldService.restartServer()
            else -> worldService.disconnect()
        }
    }

    private fun updateButtons() {
        val client = client ?: return

        val dismissButton = dismissButton ?: ButtonWidget.builder(text.string(StringKey.CardClose)) { close() }
            .width(BUTTON_WIDTH)
            .position(0, 0)
            .build()
            .also {
                dismissButton = it
            }

        dismissButton.x = (width/2) - 4 - BUTTON_WIDTH
        dismissButton.y = (height/2) + (BingoHudRenderer.CARD_HEIGHT /2) + 8
        addDrawableChild(dismissButton)

        val restartButton = restartButton ?: ButtonWidget.builder(
            when {
                client.isInSingleplayer -> text.string(StringKey.WorldNewGame)
                gameOver.packet.canSendRestart -> text.string(StringKey.WorldRestart)
                else -> text.string(StringKey.WorldDisconnect)
            }
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
        restartButton.y = (height/2) + (BingoHudRenderer.CARD_HEIGHT /2) + 8
        addDrawableChild(restartButton)
    }

    override fun init() {
        super.init()
        updateButtons()
    }

    override fun resize(client: MinecraftClient?, width: Int, height: Int) {
        super.resize(client, width, height)
        updateButtons()
    }

    private fun renderGameDuration(context: DrawContext, titleY: Int) {
        context.matrices.push()
        context.matrices.translate(width * 0.2f - 20f, titleY.toFloat(), 0f)
        context.matrices.multiply(Quaternionf(AxisAngle4f(-.25f, 0f, 0f, 1f)))
        context.matrices.push()
        context.matrices.scale(2f, 2f, 1f)

        val gameDuration = gameOver.packet.duration.formatString()
        context.drawText(
            textRenderer,
            gameDuration,
            -textRenderer.getWidth(gameDuration)/2, 0,
            0xFF_A3F5A3.toInt(),
            true,
        )

        context.matrices.pop()

        val title = text.string(StringKey.StatsGameDuration)
        context.drawText(textRenderer, title, -textRenderer.getWidth(title)/2, 18, 0xFF_FFFFFF.toInt(), true)
        context.matrices.pop()
    }

    private fun renderWinStreak(context: DrawContext, titleY: Int) {
        val winStreak = gameOver.packet.winStreak ?: return

        context.matrices.push()
        context.matrices.translate(width * 0.8f + 20f, titleY.toFloat(), 0f)
        context.matrices.multiply(Quaternionf(AxisAngle4f(.25f, 0f, 0f, 1f)))
        context.matrices.push()
        context.matrices.scale(2f, 2f, 1f)

        context.drawText(
            textRenderer,
            "$winStreak",
            -textRenderer.getWidth("$winStreak")/2, 0,
            0xFF_F5A3D6.toInt(),
            true,
        )

        context.matrices.pop()

        val title = text.string(StringKey.StatsWinStreak)
        context.drawText(textRenderer, title, -textRenderer.getWidth(title)/2, 18, 0xFF_FFFFFF.toInt(), true)
        context.matrices.pop()

        if (winStreak > 1 && gameOver.packet.isWinner) {
            context.matrices.push()
            context.matrices.translate(width * 0.8f, titleY.toFloat() + 12, 0f)
            context.matrices.multiply(Quaternionf(AxisAngle4f(
                Clock.System.now().toEpochMilliseconds()
                    .mod(2000)
                    .div(2000f)
                    .times(2*PI.toFloat()),
                0f, 0f, 1f
            )))
            context.matrices.scale(8f, 8f, 1f)
            context.matrices.push()
            context.matrices.translate(-2.5, -3.5, 0.0)

            context.drawText(
                textRenderer,
                "◆",
                0, 0,
                0x30_F5A3D6.toInt(),
                false,
            )

            context.matrices.pop()
            context.matrices.pop()
        }
    }

    override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        if (context == null) return
        try {
            renderBackground(context)
        } catch (e: NoSuchMethodError) {
            // This doesn't exist on 1.20.2+
        }

        super.render(context, mouseX, mouseY, delta)

        val cardTop = (height/2) - (BingoHudRenderer.CARD_HEIGHT/2)
        val titleY = min((cardTop * 0.75f).roundToInt(), cardTop - 11*2)

        context.matrices.push()
        context.matrices.scale(2f, 2f, 2f)
        context.drawText(
            textRenderer,
            gameOver.packet.title,
            (width/4) - (textRenderer.getWidth(gameOver.packet.title)/2),
            (titleY/2) - 9,
            0xff_ffffff.toInt(),
            true,
        )
        context.matrices.pop()

        context.drawText(
            textRenderer,
            gameOver.packet.subtitle,
            (width/2) - (textRenderer.getWidth(gameOver.packet.subtitle)/2),
            titleY + 4,
            0xff_ffffff.toInt(),
            true,
        )

        renderGameDuration(context, titleY - 9)
        renderWinStreak(context, titleY - 9)

        cardsWidget.views = gameOver.views
        cardsWidget.winner = gameOver.packet.winner
        cardsWidget.x = width/2
        cardsWidget.y = height/2
        cardsWidget.width = width * 6/10
        cardsWidget.render(context, mouseX, mouseY, delta)

        if (gameOver.packet.isWinner) {
            fireworkRenderer.render(context, textRenderer, width, height, delta)
        }
    }

    override fun shouldPause() = false

    override fun shouldCloseOnEsc() = canEscape

}