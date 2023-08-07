package me.jfenn.bingo.utils

class EventListener<T> {

    val listeners = mutableListOf<(T) -> Unit>()

    operator fun invoke(event: T) {
        listeners.forEach { it(event) }
    }

    operator fun invoke(callback: (T) -> Unit) {
        listeners.add(callback)
    }

}
