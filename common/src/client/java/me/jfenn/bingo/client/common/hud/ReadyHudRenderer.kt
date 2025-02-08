package me.jfenn.bingo.client.common.hud

import me.jfenn.bingo.client.platform.renderer.IDrawService
import me.jfenn.bingo.common.KEYBIND_OPEN_CARD
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatHHMMSS
import me.jfenn.bingo.generated.StringKey
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

internal class ReadyHudRenderer(
    private val state: BingoHudState,
    private val text: TextProvider,
) {
    companion object {
        private const val TEXT_COLOR = 0xffA3F5A3.toInt()
        private const val LOADING_COLOR = 0xffffffff.toInt()
    }

    private var progressInterpolated: Float = 0f

    private fun drawProgressBar(drawService: IDrawService, x: Int, y: Int, width: Int, height: Int, progress: Float) {
        val context = drawService.context
        progressInterpolated = (progressInterpolated.takeIf { it.isFinite() } ?: 0f) * 0.8f + progress * 0.2f

        val xProgress = x + ((width - 1) * progressInterpolated).roundToInt() + 1
        context.fill(x, y, xProgress, y + height + 1, LOADING_COLOR)
        context.drawHorizontalLine(xProgress, x + width, y, LOADING_COLOR)
        context.drawHorizontalLine(xProgress, x + width, y + height, LOADING_COLOR)
        context.drawVerticalLine(x + width, y, y + height, LOADING_COLOR)
    }

    fun drawReadyHud(drawService: IDrawService, x: Int, y: Int, width: Int) {
        val ready = state.ready ?: return
        if (!ready.isRunning) return

        val textRenderer = MinecraftClient.getInstance().textRenderer
        val context = drawService.context

        val titleText = ready.title ?: when {
            ready.isReady -> text.string(StringKey.LobbyStartingPlayersReady, ready.readyPlayers, ready.totalPlayers)
            else -> text.string(StringKey.LobbyStartingReadyUp, Text.keybind("key.sneak"), Text.keybind(KEYBIND_OPEN_CARD))
        }
        val bodyText = ready.subtitle ?: when {
            ready.state == GameState.PREGAME -> text.string(StringKey.LobbyStartingTimeRemaining, ready.remainingDuration.formatHHMMSS())
            else -> text.string(StringKey.LobbyNextRoundTimeRemaining, ready.remainingDuration.formatHHMMSS())
        }

        drawService.drawText(titleText, x + width/2 - drawService.font.getTextWidth(titleText)/2, y, LOADING_COLOR, true)
        drawService.drawText(bodyText, x + width/2 - drawService.font.getTextWidth(bodyText)/2, y + textRenderer.fontHeight + 2, TEXT_COLOR, true)

        val remainingProgress = ready.remainingDuration / ready.totalDuration.coerceAtLeast(1.seconds)
        drawProgressBar(drawService, x, y + (textRenderer.fontHeight * 2) + 5, width, 5, 1f - remainingProgress.toFloat().coerceIn(0f, 1f))

        val charCount = ready.totalPlayers.coerceAtMost(10)
        val charReady = "✔"
        val charNot = "•"
        val chars = buildList(charCount) {
            for (i in 0 until charCount) {
                val isReady = i * ready.totalPlayers < charCount * ready.readyPlayers
                val char = if (isReady) charReady else charNot
                add(
                    ReadyChar(
                        char = char,
                        color = if (isReady) TEXT_COLOR else LOADING_COLOR,
                        width = textRenderer.getWidth(char) + 2
                    )
                )
            }
        }
        val charWidth = textRenderer.getWidth(charReady)
        val charTotalWidth = charWidth * chars.size
        for ((i, char) in chars.withIndex()) {
            context.drawText(textRenderer, Text.literal(char.char), x + width/2 - charTotalWidth/2 + (i*charWidth) + (charWidth-char.width)/2, y + (textRenderer.fontHeight * 2) + 12, char.color, true)
        }
    }

    private class ReadyChar(
        val char: String,
        val color: Int,
        val width: Int,
    )
}