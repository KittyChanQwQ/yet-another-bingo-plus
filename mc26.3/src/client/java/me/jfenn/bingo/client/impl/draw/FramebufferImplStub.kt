package me.jfenn.bingo.client.impl.draw

import me.jfenn.bingo.client.platform.renderer.IDrawService
import me.jfenn.bingo.client.platform.renderer.IFramebuffer

class FramebufferImplStub(
    override val width: Int,
    override val height: Int
) : IFramebuffer {
    override fun register() {
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun write(callback: (IDrawService) -> Unit) {
    }

    override fun draw(service: IDrawService, width: Int, height: Int) {
    }

    override fun close() {
    }
}