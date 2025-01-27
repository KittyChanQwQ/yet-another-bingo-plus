package me.jfenn.bingo.platform

import me.jfenn.bingo.platform.utils.IEventListener

interface ICloseEvent {
    val onClose: IEventListener<Unit>
}