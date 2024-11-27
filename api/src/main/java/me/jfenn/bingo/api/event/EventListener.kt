package me.jfenn.bingo.api.event

import me.jfenn.bingo.api.annotations.BingoInternalApi
import org.slf4j.LoggerFactory
import java.util.function.Consumer

class EventListener<T> {
    private val callbacks = mutableListOf<Consumer<T>>()
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun register(callback: Consumer<T>) {
        callbacks.add(callback)
    }

    @BingoInternalApi
    fun invoke(event: T) {
        for (callback in callbacks) {
            try {
                callback.accept(event)
            } catch (e: Throwable) {
                logger.error("Exception in event handler", e)
            }
        }
    }
}