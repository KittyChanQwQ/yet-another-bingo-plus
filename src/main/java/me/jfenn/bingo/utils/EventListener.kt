package me.jfenn.bingo.utils

import org.slf4j.LoggerFactory

class EventListener<T> {

    private val log = LoggerFactory.getLogger(this::class.java)
    private val listeners = mutableListOf<(T) -> Unit>()

    operator fun invoke(event: T) {
        listeners.forEach {
            try {
                it(event)
            } catch (e: Throwable) {
                log.error("Exception in event handler:", e)
            }
        }
    }

    operator fun invoke(callback: (T) -> Unit) {
        listeners.add(callback)
    }

}
