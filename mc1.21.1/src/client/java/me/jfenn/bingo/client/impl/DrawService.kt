package me.jfenn.bingo.client.impl

import me.jfenn.bingo.client.api.IDrawService
import me.jfenn.bingo.client.api.IDrawServiceFactory
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.Identifier

class DrawService(
    override val context: DrawContext,
) : IDrawService {

    companion object : IDrawServiceFactory {
        override fun create(drawContext: DrawContext): IDrawService = DrawService(drawContext)
    }

    override fun drawTexture(
        texture: Identifier,
        x: Int,
        y: Int,
        z: Int,
        u: Float,
        v: Float,
        width: Int,
        height: Int,
        textureWidth: Int,
        textureHeight: Int
    ) {
        context.drawTexture(
            with(texture) { Identifier.of(namespace, "textures/gui/sprites/$path.png") },
            x, y, z,
            u, v,
            width, height,
            textureWidth, textureHeight
        )
    }

}
