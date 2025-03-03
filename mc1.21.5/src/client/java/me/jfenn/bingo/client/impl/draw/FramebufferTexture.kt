package me.jfenn.bingo.client.impl.draw

import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.texture.AbstractTexture
import net.minecraft.client.texture.DrawableTexture

class FramebufferTexture(private val fb: SimpleFramebuffer) : AbstractTexture() {
    override fun getGlTexture(): DrawableTexture? {
        return fb.colorAttachment
    }

    override fun setFilter(bilinear: Boolean, mipmap: Boolean) {
        // no-op
    }

    override fun close() {
        // no-op
    }
}