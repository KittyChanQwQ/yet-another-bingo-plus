package me.jfenn.bingo.client.api

import me.jfenn.bingo.common.utils.EventListener

interface IHudCallback {
    val onRender: EventListener<RenderEvent>

    class RenderEvent(
        val drawService: IDrawService,
        val delta: Float,
    )
}