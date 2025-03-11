package me.jfenn.bingo.client.impl.draw

import com.mojang.blaze3d.textures.GpuTexture
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.texture.AbstractTexture

class FramebufferTexture(private val fb: SimpleFramebuffer) : AbstractTexture() {
    override fun method_68004(): GpuTexture? { // getGlTexture
        return fb.method_30277() // fb.colorAttachment
    }

    override fun setFilter(bilinear: Boolean, mipmap: Boolean) {
        // no-op
    }

    override fun close() {
        // no-op
    }
}