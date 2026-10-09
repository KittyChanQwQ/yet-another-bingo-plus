package me.jfenn.bingo.client.common.hud.screen

import me.jfenn.bingo.client.common.state.BingoHudState
import me.jfenn.bingo.client.platform.IClient
import me.jfenn.bingo.client.platform.IScrollableWidget
import me.jfenn.bingo.client.platform.renderer.IDrawService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatHHMMSS
import me.jfenn.bingo.common.utils.minus
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.text.Formatting
import me.jfenn.bingo.platform.text.IText
import org.koin.core.Koin
import java.time.Instant

internal class BingoEndScoresWidget(
    koin: Koin,
    private val gameOver: BingoHudState.GameOver,
    private val client: IClient = koin.get(),
    private val text: TextProvider = koin.get(),
) : IScrollableWidget() {

    private val entryHeight = client.font.getTextHeight() + 8

    override fun measureContentHeight(): Int {
        return entryHeight * (gameOver.packet.scores.size + 1)
    }

    private val teamCol = 4
    private var scoreCol: Int = 0
    private var durationCol: Int = 0

    private val teamColTitle = text.string(StringKey.GameEndScoresTeam).formatted(Formatting.GRAY)
    private val scoreColTitle = text.string(StringKey.GameEndScoresScore).formatted(Formatting.GRAY)
    private val durationColTitle = text.string(StringKey.GameEndScoresTime).formatted(Formatting.GRAY)

    private data class ScoreEntry(
        val team: IText,
        val teamTooltip: List<IText>?,
        val score: IText,
        val duration: IText,
    ) {
        val teamObfuscated = team.copy().formatted(Formatting.OBFUSCATED)
        val scoreObfuscated = score.copy().formatted(Formatting.OBFUSCATED)
        val durationObfuscated = duration.copy().formatted(Formatting.OBFUSCATED)
    }
    private var scores: List<ScoreEntry> = emptyList()

    fun resize() {
        val updatedScores = gameOver.packet.scores.map { score ->
            val players = gameOver.cards.find { it.teamKey == score.key }?.display?.players

            val teamText = text.empty()
                .append("#${score.index+1} ")
                .append(score.name)

            val scoreText = text.empty().append(score.score.formatText(text))

            val durationText = score.duration?.formatHHMMSS()?.let { text.literal(it) }
                ?: text.string(StringKey.GameEndScoresDnf)

            ScoreEntry(
                team = teamText,
                teamTooltip = players?.map { player ->
                    text.player(player.uuid)?.let {
                        text.empty().append(it).append(" ").append(player.name)
                    } ?: text.literal(player.name)
                }?.let {
                    // if there's >1 player, include the team name in the tooltip
                    if (it.size > 1) listOf(score.name) + it else it
                },
                score = scoreText,
                duration = durationText,
            )
        }

        val teamMaxWidth = (updatedScores.map { it.team } + teamColTitle).maxOf { client.font.getTextWidth(it) }
        val scoreMaxWidth = (updatedScores.map { it.score } + scoreColTitle).maxOf { client.font.getTextWidth(it) }
        val durationMaxWidth = (updatedScores.map { it.duration } + durationColTitle).maxOf { client.font.getTextWidth(it) }

        durationCol = contentWidth - durationMaxWidth - 4
        val scoreMaxX = durationCol - scoreMaxWidth - 4
        val scoreMinX = teamCol + teamMaxWidth
        scoreCol = scoreMinX.plus(scoreMaxX).div(2).coerceAtMost(scoreMaxX)

        scores = updatedScores.map {
            it.copy(team = client.font.truncate(it.team, scoreCol - teamCol))
        }
    }

    override fun renderContents(drawService: IDrawService, mouseX: Int, mouseY: Int) {
        val yOffset = (entryHeight/2) - (client.font.getTextHeight()/2)

        drawService.drawText(
            teamColTitle,
            teamCol, yOffset,
            0xFF_FFFFFF.toInt(), true,
        )

        drawService.drawText(
            scoreColTitle,
            scoreCol, yOffset,
            0xFF_FFFFFF.toInt(), true,
        )

        drawService.drawText(
            durationColTitle,
            durationCol, yOffset,
            0xFF_FFFFFF.toInt(), true,
        )

        val now = Instant.now()
        val secondsElapsed = (now - gameOver.packet.endedAt).seconds
        for ((i, score) in scores.withIndex()) {
            val isObfuscated = secondsElapsed < (i+1).toLong()

            val teamText = if (isObfuscated) score.teamObfuscated else score.team
            val scoreText = if (isObfuscated) score.scoreObfuscated else score.score
            val durationText = if (isObfuscated) score.durationObfuscated else score.duration

            val y = (i + 1) * entryHeight + yOffset
            drawService.drawText(
                teamText,
                teamCol, y,
                0xFF_FFFFFF.toInt(), true
            )

            drawService.drawText(
                scoreText,
                scoreCol, y,
                0xFF_FFFFFF.toInt(), true
            )

            drawService.drawText(
                durationText,
                durationCol, y,
                0xFF_FFFFFF.toInt(), true
            )

            val isTextHovered = mouseX in teamCol..drawService.font.getTextWidth(teamText)
                    && mouseY in (y - yOffset)..(y - yOffset + entryHeight)
            if (isTextHovered && score.teamTooltip != null) {
                drawService.drawTooltip(score.teamTooltip, mouseX, mouseY)
            }
        }
    }
}