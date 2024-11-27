package me.jfenn.bingo.common.utils

import me.jfenn.bingo.platform.utils.ICallbackHandle
import me.jfenn.bingo.platform.utils.IReturnEventListener
import org.slf4j.LoggerFactory

class ReturnEventListener<T, R> : IReturnEventListener<T, R> {

    private val log = LoggerFactory.getLogger(this::class.java)
    private val listeners = mutableListOf<(T) -> R>()

    override fun invoke(event: T): List<R?> {
        return listeners.map {
            try {
                it(event)
            } catch (e: Throwable) {
                log.error("Exception in event handler:", e)
                null
            }
        }
    }

    override fun invoke(callback: (T) -> R): CallbackHandle<T, R> {
        listeners.add(callback)
        return CallbackHandle(callback)
    }

    override fun unregister(callback: ICallbackHandle) {
        if (callback is CallbackHandle<*, *>) {
            unregister(
                @Suppress("UNCHECKED_CAST")
                (callback.callback as (T) -> R)
            )
        }
    }

    private fun unregister(callback: (T) -> R) {
        listeners.remove(callback)
    }

    class CallbackHandle<T, R>(
        internal val callback: (T) -> R
    ): ICallbackHandle
}

typealias EventListener<T> = ReturnEventListener<T, Unit>
