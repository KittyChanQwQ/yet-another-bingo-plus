package me.jfenn.bingo.client.common.hud

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jfenn.bingo.common.map.Color
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import org.joml.AxisAngle4f
import org.joml.Quaternionf
import org.joml.Vector2d
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

internal class BingoEndFireworkRenderer(
    private val startedAt: Instant = Clock.System.now(),
    private var range: Int = 500,
) {

    private val starVelocityScale = 1.5

    private val colors = listOf(
        0xFF_F5C0A3.toInt(),
        0xFF_A3F5A3.toInt(),
        0xFF_A3F5C7.toInt(),
        0xFF_A3ABF5.toInt(),
        0xFF_CCA3F5.toInt(),
        0xFF_F5A3D6.toInt(),
    )

    class StarInfo(
        val x: Double,
        val y: Double,
        val rot: Double,
        val xVel: Double,
        val yVel: Double,
        val rotVel: Double,
        val color: Color,
    )

    private val stars = MutableList(50) {
        val theta = Math.random() * PI * 2
        val vel = 1 + Math.random()
        StarInfo(
            x = 0.0,
            y = 0.0,
            rot = Math.random() * PI * 2,
            xVel = cos(theta) * vel * starVelocityScale,
            yVel = sin(theta) * vel * starVelocityScale - 0.2,
            rotVel = (Math.random() - 0.5) * 0.5 * starVelocityScale,
            color = Color.fromInt(colors.random()),
        )
    }

    fun isDone() = stars.isEmpty()

    fun render(
        context: DrawContext,
        textRenderer: TextRenderer,
        centerX: Int,
        centerY: Int,
        now: Instant,
    ) {
        val ticks = (now - startedAt).inWholeMilliseconds.toDouble() / 50.0

        val iterator = stars.iterator()
        for (star in iterator) {
            val x = star.x + star.xVel * ticks
            val y = star.y + star.yVel * ticks
            val rot = star.rot + star.rotVel * ticks
            val alpha = 1f - (Vector2d(x, y).length() / range).toFloat().pow(3)

            if (alpha <= 0.025f) {
                iterator.remove()
                continue
            }

            context.matrices.push()
            context.matrices.translate(centerX.toDouble(), centerY.toDouble(), 0.0)
            context.matrices.translate(x, y, 2000.0)
            context.matrices.multiply(Quaternionf(AxisAngle4f(rot.toFloat(), 0f, 0f, 1f)))

            val color = star.color
                .copy(a = (alpha * 255).toInt().coerceIn(1, 255))
                .asIntWithAlpha

            context.drawText(textRenderer, "★", -4, -4, color, false)

            context.matrices.pop()
        }
    }

}