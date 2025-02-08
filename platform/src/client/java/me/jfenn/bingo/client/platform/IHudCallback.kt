package me.jfenn.bingo.client.platform

import me.jfenn.bingo.client.platform.renderer.IDrawService
import me.jfenn.bingo.platform.utils.IEventListener

interface IHudCallback {
    val onRender: IEventListener<RenderEvent>

    class RenderEvent(
        val drawService: IDrawService,
        val delta: Float,
    )
}