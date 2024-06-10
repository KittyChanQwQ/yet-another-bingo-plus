package me.jfenn.bingo.client.common.hud

import me.jfenn.bingo.client.api.IWorldService
import me.jfenn.bingo.common.game.GameOverPacket
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import org.koin.core.Koin
import kotlin.math.min
import kotlin.math.roundToInt

class BingoEndScreen(
    koin: Koin,
    private val gameOver: GameOverPacket,
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

    private var dismissButton: ButtonWidget? = null
    private var restartButton: ButtonWidget? = null

    private fun reopenWorld() {
        val client = client ?: return
        when {
            client.isInSingleplayer -> worldService.disconnectAndReopen()
            gameOver.canSendRestart -> worldService.restartServer()
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
                gameOver.canSendRestart -> text.string(StringKey.WorldRestart)
                else -> text.string(StringKey.WorldDisconnect)
            }
        ) {
            it.active = false;
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

        context.matrices.push()
        context.matrices.scale(2f, 2f, 2f)
        context.drawText(
            textRenderer,
            gameOver.title,
            (width/8) - (textRenderer.getWidth(gameOver.title)/2),
            (titleY/4) - 9,
            0xff_ffffff.toInt(),
            false,
        )
        context.matrices.pop()

        context.drawText(
            textRenderer,
            gameOver.subtitle,
            (width/4) - (textRenderer.getWidth(gameOver.subtitle)/2),
            (titleY/2) + 2,
            0xff_ffffff.toInt(),
            false,
        )

        context.matrices.pop()

        cardsWidget.x = width/2
        cardsWidget.y = height/2
        cardsWidget.width = width * 6/10
        cardsWidget.render(context, mouseX, mouseY, delta)
    }

    override fun shouldPause() = false

    override fun shouldCloseOnEsc() = canEscape

}