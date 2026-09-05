package me.jfenn.bingo.client.impl

import me.jfenn.bingo.client.platform.INativeImage
import me.jfenn.bingo.client.platform.INativeImageFactory
import me.jfenn.bingo.common.MOD_ID_BINGO
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier

object NativeImageFactory : INativeImageFactory {
    private val textureManager get() = Minecraft.getInstance().textureManager
    private var id: Int = 0

    override fun create(width: Int, height: Int): INativeImage {
        val id = "item_${id++}"
        val texture = DynamicTexture(id, width, height, true)
        val textureId = Identifier.fromNamespaceAndPath(MOD_ID_BINGO, id)
        textureManager.register(textureId, texture)
        return NativeImageImpl(texture, textureId, width, height)
    }
}

class NativeImageImpl(
    private val texture: DynamicTexture,
    override val textureId: Identifier,
    override val width: Int,
    override val height: Int,
) : INativeImage {

    // internal val renderLayer by lazy {
    //     RenderLayers.text(textureId)
    // }

    internal val glTextureView get() = texture.textureView

    internal val sampler get() = texture.sampler

    override fun setPixel(x: Int, y: Int, color: Int) {
        texture.pixels.setPixel(x, y, color)
    }

    override fun upload() {
        texture.upload()
    }

    override fun close() {
        texture.close()
    }
}
