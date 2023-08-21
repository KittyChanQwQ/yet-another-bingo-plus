package me.jfenn.bingo.utils

import java.time.Duration
import java.util.*

/**
 * Returns a human-readable length of the duration, in the format:
 * "2h 01m 32s"
 *
 * Omits the hour portion of the string if <= 0.
 */
fun Duration.formatString(): String {
    val secondsRemainingStr = String.format(Locale.US, "%02dm %02ds", toMinutesPart(), toSecondsPart())
    return "${toHoursPart().takeIf { it > 0 }?.let { "${it}h " }.orEmpty()}$secondsRemainingStr"
}
