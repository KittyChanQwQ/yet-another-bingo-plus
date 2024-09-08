package me.jfenn.bingo.client.common.hud

import kotlinx.datetime.Clock
import me.jfenn.bingo.client.api.IDrawService
import me.jfenn.bingo.client.api.IScrollableWidget
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatHHMMSS
import me.jfenn.bingo.generated.StringKey
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.koin.core.Koin

class BingoEndScoresWidget(
    koin: Koin,
    private val gameOver: BingoHudState.GameOver,
    private val text: TextProvider = koin.get(),
) : IScrollableWidget() {

    private val textRenderer = MinecraftClient.getInstance().textRenderer
    private val entryHeight = textRenderer.fontHeight + 8

    override fun measureContentHeight(): Int {
        return entryHeight * (gameOver.packet.scores.size + 1)
    }

    override fun renderContents(drawService: IDrawService, mouseX: Int, mouseY: Int) {
        val teamCol = 4
        val scoreCol = (contentWidth * 0.4f).toInt()
        val timeCol = (contentWidth * 0.8f).toInt()

        val yOffset = (entryHeight/2) - (textRenderer.fontHeight/2)

        drawService.context.drawText(
            textRenderer,
            text.string(StringKey.GameEndScoresTeam).formatted(Formatting.GRAY),
            teamCol, yOffset,
            0xFF_FFFFFF.toInt(), true,
        )

        drawService.context.drawText(
            textRenderer,
            text.string(StringKey.GameEndScoresScore).formatted(Formatting.GRAY),
            scoreCol, yOffset,
            0xFF_FFFFFF.toInt(), true,
        )

        drawService.context.drawText(
            textRenderer,
            text.string(StringKey.GameEndScoresTime).formatted(Formatting.GRAY),
            timeCol, yOffset,
            0xFF_FFFFFF.toInt(), true,
        )

        val now = Clock.System.now()
        for ((i, score) in gameOver.packet.scores.withIndex()) {
            val isObfuscated = (now - gameOver.packet.endedAt).inWholeSeconds < (i+1).toLong()

            val teamText = Text.empty()
                .append("#${i+1} ")
                .append(score.name)
                .let { if (isObfuscated) it.formatted(Formatting.OBFUSCATED) else it }

            val scoreText = Text.empty()
                .append(score.score.formatText(text))
                .let { if (isObfuscated) it.formatted(Formatting.OBFUSCATED) else it }

            val durationText = Text.literal(score.duration?.formatHHMMSS() ?: "DNF")
                .let { if (isObfuscated) it.formatted(Formatting.OBFUSCATED) else it }

            val y = (i + 1) * entryHeight + yOffset
            drawService.context.drawText(
                textRenderer,
                teamText,
                teamCol, y,
                0xFF_FFFFFF.toInt(), true
            )

            drawService.context.drawText(
                textRenderer,
                scoreText,
                scoreCol, y,
                0xFF_FFFFFF.toInt(), true
            )

            drawService.context.drawText(
                textRenderer,
                durationText,
                timeCol, y,
                0xFF_FFFFFF.toInt(), true
            )

            val isTextHovered = mouseX in teamCol..textRenderer.getWidth(teamText)
                    && mouseY in (y - yOffset)..(y - yOffset + entryHeight)
            if (isTextHovered) {
                val view = gameOver.views.find { it.teamKey == score.key }
                if (view != null) {
                    drawService.context.matrices.push()
                    drawService.context.matrices.translate(0f, 0f, 2000f)
                    drawService.context.drawTooltip(textRenderer, view.players.map { Text.literal(it.name) }, mouseX, mouseY)
                    drawService.context.matrices.pop()
                }
            }
        }
    }
}