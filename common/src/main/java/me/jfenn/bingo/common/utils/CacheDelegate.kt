package me.jfenn.bingo.common.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

class CacheDelegateArg<T, A>(
    private val timeout: Duration,
    private val getter: (A) -> T,
) {
    private var lastExecution: Instant? = null
    private var lastValue: T? = null

    fun get(arg: A): T {
        val now = Clock.System.now()
        return if (lastExecution?.let { it + timeout > now } == true) {
            lastExecution = now
            // lastValue should never be null here (unless T is nullable and getter returned null)
            (@Suppress("UNCHECKED_CAST") (lastValue as T))
        } else {
            val value = getter(arg)
            lastExecution = now
            lastValue = value
            value
        }
    }
}

fun <T, A> cacheFor(timeout: Duration, getter: (A) -> T) = CacheDelegateArg(timeout, getter)
