package me.jfenn.bingo.platform.utils

interface ICallbackHandle

interface IReturnEventListener<T, R> {
    operator fun invoke(event: T): List<R?>
    operator fun invoke(callback: (T) -> R): ICallbackHandle
    fun unregister(callback: ICallbackHandle)
}

typealias IEventListener<T> = IReturnEventListener<T, Unit>
