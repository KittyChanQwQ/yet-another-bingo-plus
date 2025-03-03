package me.jfenn.bingo.client.impl.draw

import com.mojang.blaze3d.systems.RenderSystem
import me.jfenn.bingo.client.impl.NativeImageImpl
import me.jfenn.bingo.client.platform.INativeImage
import me.jfenn.bingo.client.platform.renderer.IDrawService
import me.jfenn.bingo.client.platform.renderer.IDrawServiceFactory
import me.jfenn.bingo.client.platform.renderer.IFont
import me.jfenn.bingo.client.platform.renderer.IMatrixStack
import me.jfenn.bingo.client.platform.text.IText
import me.jfenn.bingo.impl.ItemStackFactory
import me.jfenn.bingo.platform.item.IItemStack
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
<<<<<<<
=======
import net.minecraft.client.gl.SimpleFramebuffer
>>>>>>>
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.BufferBuilderStorage
import net.minecraft.client.render.RenderLayer
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier
import org.joml.Matrix4f

class DrawService(
    override val context: DrawContext,
) : IDrawService {

    override val textRenderer: TextRenderer = MinecraftClient.getInstance().textRenderer
    override val font: IFont = FontImpl(textRenderer)
    override val matrices: IMatrixStack = MatrixStackImpl(context.matrices)

    companion object : IDrawServiceFactory {
        override fun create(drawContext: DrawContext): IDrawService = DrawService(drawContext)
<<<<<<<
    }
=======

        private val bufferBuilderStorage by lazy {
            BufferBuilderStorage(Runtime.getRuntime().availableProcessors())
        }

        override fun newBuffer(width: Int, height: Int): IFramebuffer {
            val client = MinecraftClient.getInstance()
            val buffer = SimpleFramebuffer(width, height, true)
            val context = DrawContext(client, bufferBuilderStorage.entityVertexConsumers)
>>>>>>>

    override fun enableBlend() {}

    override fun disableBlend() {}

    override fun setShaderColor(r: Float, g: Float, b: Float, a: Float) {
        context.draw()
        RenderSystem.setShaderColor(r, g, b, a)
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