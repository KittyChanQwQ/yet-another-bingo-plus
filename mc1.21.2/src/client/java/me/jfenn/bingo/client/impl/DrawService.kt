package me.jfenn.bingo.client.impl

import com.mojang.blaze3d.systems.RenderSystem
import me.jfenn.bingo.client.api.IDrawService
import me.jfenn.bingo.client.api.IDrawServiceFactory
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Identifier

class DrawService(
    override val context: DrawContext,
) : IDrawService {

    companion object : IDrawServiceFactory {
        override fun create(drawContext: DrawContext): IDrawService = DrawService(drawContext)
    }

    override fun setShaderColor(r: Float, g: Float, b: Float, a: Float) {
        context.draw()
        RenderSystem.setShaderColor(r, g, b, a)
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
        context.matrices.push()
        context.matrices.translate(0.0, 0.0, z.toDouble())
        try {
            context.drawGuiTexture(
                RenderLayer::getGuiTextured,
                texture,
                x, y,
                width, height,
            )
        } finally {
            context.matrices.pop()
        }
    }

}
