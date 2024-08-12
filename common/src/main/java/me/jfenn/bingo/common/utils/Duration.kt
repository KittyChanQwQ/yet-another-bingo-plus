package me.jfenn.bingo.common.utils

import java.util.*
import kotlin.time.Duration

/**
 * Returns a human-readable length of the duration, in the format:
 * "3d 2h 01m 32s"
 *
 * Omits the days/hours portion of the string if <= 0.
 */
fun Duration.formatString(): String {
    val sign = if (isNegative()) "-" else ""

    return absoluteValue.toComponents { days, hours, minutes, seconds, _ ->
        (
            sign
            + "${days}d ".takeIf { days > 0 }.orEmpty()
            + "${hours}h ".takeIf { hours > 0 }.orEmpty()
            + String.format(Locale.US, "%02dm %02ds", minutes, seconds)
        )
    }
}

/**
 * Returns a human-readable length of the duration, in the format:
 * "2h" or "1m" or "1s", depending on the largest unit
 */
fun Duration.formatStringSmall(): String {
    val sign = if (isNegative()) "-" else ""

    return absoluteValue.toComponents { days, hours, minutes, seconds, _ ->
        sign + when {
            days > 0 -> "${days}d"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }
}

fun Duration.formatHHMMSS(): String {
    return toComponents { _, hours, minutes, seconds, _ ->
        when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%d:%02d", minutes, seconds)
        }
    }
}
