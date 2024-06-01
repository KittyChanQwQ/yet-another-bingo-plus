package me.jfenn.bingo.common.utils

import org.slf4j.Logger
import kotlin.time.TimeSource

inline fun Logger.measureTime(
    message: String,
    callback: () -> Unit,
) {
    val start = TimeSource.Monotonic.markNow()
    info(message)

    try {
        callback()
    } finally {
        info("$message - done in ${start.elapsedNow()}!")
    }
}