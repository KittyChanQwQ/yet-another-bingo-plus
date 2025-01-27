package me.jfenn.bingo.common.utils

import me.jfenn.bingo.platform.utils.ICallbackHandle
import me.jfenn.bingo.platform.utils.IReturnEventListener
import org.slf4j.LoggerFactory
import kotlin.reflect.KProperty

class ReturnEventListener<T, R>(
    private val label: String? = null,
) : IReturnEventListener<T, R> {

    private val log = LoggerFactory.getLogger(this::class.java)
    private val listeners = mutableListOf<(T) -> R>()

    override fun invoke(event: T): List<R?> {
        return listeners.map {
            try {
                it(event)
            } catch (e: Throwable) {
                log.error("Exception in event handler ($label):", e)
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

    inner class CallbackHandle<T, R>(
        internal val callback: (T) -> R
    ): ICallbackHandle {
        override fun close() {
            unregister(this)
        }
    }
}

typealias EventListener<T> = ReturnEventListener<T, Unit>

class EventListenerFactory<T, R> {
    private var listener: ReturnEventListener<T, R>? = null
    operator fun getValue(thisRef: Any?, property: KProperty<*>): ReturnEventListener<T, R> {
        listener?.let { return it }

        return synchronized(this) {
            // this seems redundant, but it isn't
            listener?.let { return it }

            // construct a new listener if one doesn't exist
            val newListener = ReturnEventListener<T, R>(label = property.name)
            listener = newListener
            newListener
        }
    }
}
