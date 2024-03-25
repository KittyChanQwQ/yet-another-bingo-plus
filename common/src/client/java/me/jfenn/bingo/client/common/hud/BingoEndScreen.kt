package me.jfenn.bingo.client.common.hud

import me.jfenn.bingo.client.api.IDrawServiceFactory
import me.jfenn.bingo.client.common.world.BingoWorldService
import me.jfenn.bingo.common.map.CardView
import me.jfenn.bingo.common.text.GameOverPacket
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import org.koin.core.Koin
import kotlin.math.ceil

class BingoEndScreen(
    koin: Koin,
    private val gameOver: GameOverPacket,
    private val canEscape: Boolean = false,
    private val text: TextProvider = koin.get(),
) : Screen(text.string(StringKey.CardTitle)) {

    private val drawServiceFactory = koin.get<IDrawServiceFactory>()
    private val state = koin.get<BingoHudState>()
    private val renderer = koin.get<BingoHudRenderer>()
    private val worldService = koin.get<BingoWorldService>()

    companion object {
        private const val BUTTON_WIDTH = 120
    }

    private fun viewsWithPositions(callback: (view: CardView, x: Int, y: Int) -> Unit) {
        val views = (state.display?.display ?: emptyList())
            .mapNotNull { state.views[it] }

        val rows = 1
        val columns = ceil(views.size / rows.toFloat()).toInt()

        val screenX = width/2 - (BingoHudRenderer.CARD_WIDTH * columns)/2
        val screenY = height/2 - (BingoHudRenderer.CARD_HEIGHT * rows)/2

        for ((i, view) in views.withIndex()) {
            val col = i % columns
            val row = i / columns

            val x = screenX + (col * BingoHudRenderer.CARD_WIDTH)
            val y = screenY + (row * BingoHudRenderer.CARD_HEIGHT)
            callback(view, x, y)
        }
    }

    var dismissButton: ButtonWidget? = null
    var restartButton: ButtonWidget? = null

    fun reopenWorld() {
        val client = client ?: return
        when {
            client.isInSingleplayer -> worldService.reopenClientWorld()
            gameOver.canSendRestart -> {
                client.player?.networkHandler?.sendCommand("restart")
            }
            else -> worldService.disconnect()
        }
    }

    fun updateButtons() {
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

        context.matrices.push()
        context.matrices.scale(2f, 2f, 2f)

        context.matrices.push()
        context.matrices.scale(2f, 2f, 2f)
        context.drawText(
            textRenderer,
            gameOver.title,
            (width/8) - (textRenderer.getWidth(gameOver.title)/2),
            (height/8) - (BingoHudRenderer.CARD_HEIGHT/8) - 16,
            0xff_ffffff.toInt(),
            false,
        )
        context.matrices.pop()

        context.drawText(
            textRenderer,
            gameOver.subtitle,
            (width/4) - (textRenderer.getWidth(gameOver.subtitle)/2),
            (height/4) - (BingoHudRenderer.CARD_HEIGHT/4) - 14,
            0xff_ffffff.toInt(),
            false,
        )

        context.matrices.pop()

        val drawService = drawServiceFactory.create(context)

        viewsWithPositions { view, x, y ->
            renderer.drawCard(
                context,
                drawService,
                view,
                x = x,
                y = y,
                cardScale = 1f,
                mouseX = mouseX,
                mouseY = mouseY,
                showTeamOutlines = false,
            )
        }
    }

    override fun shouldPause() = false

    override fun shouldCloseOnEsc() = canEscape

}