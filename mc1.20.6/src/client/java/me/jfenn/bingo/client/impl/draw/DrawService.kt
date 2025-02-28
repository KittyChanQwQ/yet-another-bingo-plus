package me.jfenn.bingo.client.impl.draw

import com.mojang.blaze3d.systems.RenderSystem
import me.jfenn.bingo.client.impl.NativeImageImpl
import me.jfenn.bingo.client.platform.INativeImage
import me.jfenn.bingo.client.platform.renderer.*
import me.jfenn.bingo.client.platform.text.IText
import me.jfenn.bingo.impl.ItemStackFactory
import me.jfenn.bingo.platform.item.IItemStack
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier

class DrawService(
    override val context: DrawContext,
) : IDrawService {

    override val textRenderer: TextRenderer = MinecraftClient.getInstance().textRenderer
    override val font: IFont = FontImpl(textRenderer)
    override val matrices: IMatrixStack = MatrixStackImpl(context.matrices)

    companion object : IDrawServiceFactory {
        override fun create(drawContext: DrawContext): IDrawService = DrawService(drawContext)

        private val vertexConsumers by lazy {
            VertexConsumerProvider.immediate(BufferBuilder(1536))
        }

        override fun newBuffer(width: Int, height: Int): IFramebuffer {
            val client = MinecraftClient.getInstance()
            val buffer = SimpleFramebuffer(width, height, true, MinecraftClient.IS_SYSTEM_MAC)
            val context = DrawContext(client, vertexConsumers)

            return FramebufferImpl(
                buffer, DrawService(context)
            )
        }
    }

    override fun enableBlend() {
        RenderSystem.enableBlend()
    }

    override fun disableBlend() {
        RenderSystem.disableBlend()
    }

    override fun setShaderColor(r: Float, g: Float, b: Float, a: Float) {
        context.setShaderColor(r, g, b, a)
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
        context.drawItemInSlot(textRenderer, stack.stack, x, y)
    }

    override fun drawDynamicTexture(texture: INativeImage, x: Int, y: Int, width: Int, height: Int) {
        require(texture is NativeImageImpl)
        context.drawTexture(texture.textureId, x, y, 0, 0f, 0f, width, height, texture.width, texture.height)
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
        context.drawTexture(
            with(texture) { Identifier(namespace, "textures/gui/sprites/$path.png") },
            x, y, z,
            u, v,
            width, height,
            textureWidth, textureHeight
        )
    }

}