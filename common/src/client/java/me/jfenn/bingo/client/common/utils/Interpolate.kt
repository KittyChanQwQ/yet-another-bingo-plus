package me.jfenn.bingo.client.common.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.PI
import kotlin.math.cos
import kotlin.time.Duration

class Interpolate(
    var from: Float,
    var to: Float,
    var duration: Duration = Duration.ZERO,
    var startedAt: Instant = Clock.System.now(),
) {
    enum class Easing {
        LINEAR,
        IN_OUT
    }

    fun progress(
        easing: Easing = Easing.LINEAR,
        now: Instant = Clock.System.now(),
    ): Float {
        val animTimeSince = now - startedAt
        val progress = (animTimeSince / duration).toFloat().coerceIn(0f, 1f)
        return when (easing) {
            Easing.LINEAR -> progress
            Easing.IN_OUT -> -(cos(PI * progress()) - 1).toFloat() / 2f
        }
    }

    fun isDone() = progress() > 0.99f

    fun get(
        easing: Easing = Easing.LINEAR,
        now: Instant = Clock.System.now(),
    ): Float {
        val progress = progress(easing, now)
        return (from * (1f - progress)) + (to * progress)
    }
}