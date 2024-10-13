package me.jfenn.bingo.client.api

import me.jfenn.bingo.common.utils.IEventListener

interface IHudCallback {
    val onRender: IEventListener<RenderEvent>

    class RenderEvent(
        val drawService: IDrawService,
        val delta: Float,
    )
}