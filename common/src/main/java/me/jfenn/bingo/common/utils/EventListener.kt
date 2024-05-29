package me.jfenn.bingo.common.utils

import org.slf4j.LoggerFactory

class ReturnEventListener<T, R> {

    private val log = LoggerFactory.getLogger(this::class.java)
    private val listeners = mutableListOf<(T) -> R>()

    operator fun invoke(event: T): List<R?> {
        return listeners.map {
            try {
                it(event)
            } catch (e: Throwable) {
                log.error("Exception in event handler:", e)
                null
            }
        }
    }

    operator fun invoke(callback: (T) -> R) {
        listeners.add(callback)
    }

    fun unregister(callback: (T) -> R) {
        listeners.remove(callback)
    }

}

typealias EventListener<T> = ReturnEventListener<T, Unit>

class ContextEventListener<C, T> {

    private val log = LoggerFactory.getLogger(this::class.java)
    private val listeners = mutableListOf<C.(T) -> Unit>()

    operator fun invoke(ctx: C, event: T) {
        listeners.forEach {
            try {
                it(ctx, event)
            } catch (e: Throwable) {
                log.error("Exception in event handler:", e)
            }
        }
    }

    operator fun invoke(callback: C.(T) -> Unit) {
        listeners.add(callback)
    }

}
