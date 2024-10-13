package me.jfenn.bingo.common.utils

import org.slf4j.LoggerFactory

interface ICallbackHandle

interface IReturnEventListener<T, R> {
    operator fun invoke(event: T): List<R?>
    operator fun invoke(callback: (T) -> R): ICallbackHandle
    fun unregister(callback: ICallbackHandle)
}

typealias IEventListener<T> = IReturnEventListener<T, Unit>

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

    fun unregister(callback: (T) -> R) {
        listeners.remove(callback)
    }

    class CallbackHandle<T, R>(
        internal val callback: (T) -> R
    ): ICallbackHandle
}

typealias EventListener<T> = ReturnEventListener<T, Unit>
