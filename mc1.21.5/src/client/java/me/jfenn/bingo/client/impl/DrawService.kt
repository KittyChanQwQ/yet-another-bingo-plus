package me.jfenn.bingo.client.impl

import com.mojang.blaze3d.systems.RenderSystem
import me.jfenn.bingo.client.platform.IDrawService
import me.jfenn.bingo.client.platform.IDrawServiceFactory
import me.jfenn.bingo.client.platform.INativeImage
import me.jfenn.bingo.client.platform.text.IText
import me.jfenn.bingo.impl.ItemStackFactory
import me.jfenn.bingo.platform.item.IItemStack
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier
import org.joml.Matrix4f

class DrawService(
    override val context: DrawContext,
) : IDrawService {

    override val textRenderer: TextRenderer = MinecraftClient.getInstance().textRenderer

    companion object : IDrawServiceFactory {
        override fun create(drawContext: DrawContext): IDrawService = DrawService(drawContext)
    }

    override fun setShaderColor(r: Float, g: Float, b: Float, a: Float) {
        context.draw()
        RenderSystem.setShaderColor(r, g, b, a)
    }

    override fun getTextWidth(text: IText): Int {
        return textRenderer.getWidth(text.value)
    }

    override fun getTextHeight(): Int {
        return textRenderer.fontHeight
    }

    override fun drawText(text: IText, x: Int, y: Int, color: Int, shadow: Boolean) {
        context.drawText(textRenderer, text.value, x, y, color, shadow)
    }

    override fun drawItemStack(stack: IItemStack, x: Int, y: Int, seed: Int) {
        require(stack is ItemStackFactory.ItemStackImpl)
        val itemStack: ItemStack = stack.stack
        context.drawItem(itemStack, x, y, seed)

        // draws the item count + durability bar
        val textRenderer = MinecraftClient.getInstance().textRenderer
        context.drawStackOverlay(textRenderer, stack.stack, x, y)
    }

    override fun drawDynamicTexture(texture: INativeImage, x: Int, y: Int, width: Int, height: Int) {
        require(texture is NativeImageImpl)
        val matrix = context.matrices.peek().positionMatrix
            .let { Matrix4f(it) }
            .translate(x.toFloat(), y.toFloat(), 0f)

        val vertexConsumer = context.vertexConsumers.getBuffer(texture.renderLayer)
        vertexConsumer.vertex(matrix, 0f, height.toFloat(), 0f).color(-1).texture(0.0f, 1.0f)
        vertexConsumer.vertex(matrix, width.toFloat(), height.toFloat(), 0f).color(-1).texture(1.0f, 1.0f)
        vertexConsumer.vertex(matrix, width.toFloat(), 0f, 0f).color(-1).texture(1.0f, 0.0f)
        vertexConsumer.vertex(matrix, 0f, 0f, 0f).color(-1).texture(0.0f, 0.0f)
    }

    override fun drawGuiTexture(
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
            context.drawTexture(
                RenderLayer::getGuiTextured,
                texture.let { Identifier.of(it.namespace, "textures/gui/sprites/${it.path}.png")!! },
                x, y,
                u, v,
                width, height,
                textureWidth, textureHeight,
            )
        } finally {
            context.matrices.pop()
        }
    }

}
