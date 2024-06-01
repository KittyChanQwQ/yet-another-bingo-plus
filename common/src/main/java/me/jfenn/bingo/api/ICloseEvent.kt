package me.jfenn.bingo.api

import me.jfenn.bingo.common.utils.EventListener

interface ICloseEvent {
    val onClose: EventListener<Unit>
}