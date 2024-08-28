package me.jfenn.bingo.client.api

import net.minecraft.util.Identifier

interface INativeImageFactory {
    fun create(width: Int, height: Int): INativeImage
}

interface INativeImage: AutoCloseable {
    val textureId: Identifier
    fun setPixel(x: Int, y: Int, color: Int)
    fun upload()
}
