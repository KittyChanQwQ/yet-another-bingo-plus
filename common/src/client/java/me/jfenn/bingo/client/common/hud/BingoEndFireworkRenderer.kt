package me.jfenn.bingo.client.common.hud

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import org.joml.AxisAngle4f
import org.joml.Quaternionf
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class BingoEndFireworkRenderer {

    private val starDeceleration = 0.9
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
        var x: Double,
        var y: Double,
        var rot: Double,
        var xVel: Double,
        var yVel: Double,
        var rotVel: Double,
        val color: Int,
    )

    private val stars = List(50) {
        val theta = Math.random() * PI * 2
        val vel = 1 + Math.random()
        StarInfo(
            x = 0.0,
            y = 0.0,
            rot = Math.random() * PI * 2,
            xVel = cos(theta) * vel * starVelocityScale,
            yVel = sin(theta) * vel * starVelocityScale,
            rotVel = (Math.random() - 0.5) * 0.5 * starVelocityScale,
            color = colors.random(),
        )
    }

    fun render(
        context: DrawContext,
        textRenderer: TextRenderer,
        width: Int,
        height: Int,
        delta: Float,
    ) {
        for (star in stars) {
            star.x += star.xVel * delta
            star.y += star.yVel * delta
            star.rot += star.rotVel * delta

            val deceleration = (starDeceleration * (1f / delta)).coerceAtMost(1.0)
            star.xVel *= deceleration
            star.yVel *= deceleration
            star.rotVel *= deceleration

            star.y += 0.2

            context.matrices.push()
            context.matrices.translate(width/2.0, height/2.0, 0.0)
            context.matrices.translate(star.x, star.y, 2000.0)
            context.matrices.multiply(Quaternionf(AxisAngle4f(star.rot.toFloat(), 0f, 0f, 1f)))

            context.drawText(textRenderer, "★", -4, -4, star.color, false)

            context.matrices.pop()
        }
    }

}