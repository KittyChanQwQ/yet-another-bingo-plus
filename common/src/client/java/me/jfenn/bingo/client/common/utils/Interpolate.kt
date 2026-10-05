package me.jfenn.bingo.client.common.utils

import me.jfenn.bingo.common.utils.div
import me.jfenn.bingo.common.utils.minus
import java.time.Duration
import java.time.Instant
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow

class Interpolate(
    var from: Float,
    var to: Float,
    var duration: Duration = Duration.ZERO,
    var startedAt: Instant = Instant.now(),
    var easing: Easing = Easing.LINEAR,
) {
    enum class Easing {
        LINEAR,
        IN_OUT,
        IN_OUT_REVERSE,
        EXP_OUT,
        EXP_OUT_REVERSE,
    }

    private fun easeInOut(progress: Float) = -(cos(PI * progress) - 1).toFloat() / 2f
    private fun expOut(progress: Float) = 1f - 2f.pow(-10 * progress)

    fun progress(
        easing: Easing = this.easing,
        now: Instant = Instant.now(),
    ): Float {
        val animTimeSince = now - startedAt
        val progress = (animTimeSince / duration).toFloat().coerceIn(0f, 1f)
        return when (easing) {
            Easing.LINEAR -> progress
            Easing.IN_OUT -> easeInOut(progress)
            Easing.IN_OUT_REVERSE -> 1f - easeInOut(progress)
            Easing.EXP_OUT -> expOut(progress)
            Easing.EXP_OUT_REVERSE -> 1f - expOut(progress)
        }
    }

    fun isDone() = Instant.now() > startedAt + duration

    fun get(
        easing: Easing = this.easing,
        now: Instant = Instant.now(),
    ): Float {
        val progress = progress(easing, now)
        return (from * (1f - progress)) + (to * progress)
    }

    private var isFinished = false
    fun update(): Boolean {
        val ret = !isFinished
        isFinished = isDone()
        return ret
    }
}