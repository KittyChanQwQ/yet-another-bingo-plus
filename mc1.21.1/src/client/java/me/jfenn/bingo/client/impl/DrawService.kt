package me.jfenn.bingo.client.impl

import me.jfenn.bingo.api.item.IItemStack
import me.jfenn.bingo.client.api.IDrawService
import me.jfenn.bingo.client.api.IDrawServiceFactory
import me.jfenn.bingo.impl.ItemStackFactory
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier
import org.joml.Matrix4f

class DrawService(
    override val context: DrawContext,
) : IDrawService {

    companion object : IDrawServiceFactory {
        override fun create(drawContext: DrawContext): IDrawService = DrawService(drawContext)
    }

    override fun setShaderColor(r: Float, g: Float, b: Float, a: Float) {
        context.setShaderColor(r, g, b, a)
    }

    override fun drawItemStack(stack: IItemStack, x: Int, y: Int, seed: Int) {
        require(stack is ItemStackFactory.ItemStackImpl)
        val itemStack: ItemStack = stack.stack
        context.drawItem(itemStack, x, y, seed)

        // draws the item count + durability bar
        val textRenderer = MinecraftClient.getInstance().textRenderer
        context.drawItemInSlot(textRenderer, stack.stack, x, y)
    }

    override fun drawDynamicTexture(texture: Identifier, x: Int, y: Int, width: Int, height: Int) {
        val matrix = context.matrices.peek().positionMatrix
            .let { Matrix4f(it) }
            .translate(x.toFloat(), y.toFloat(), 0f)

        val vertexConsumer = context.vertexConsumers.getBuffer(RenderLayer.getGui())
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
        context.drawTexture(
            with(texture) { Identifier.of(namespace, "textures/gui/sprites/$path.png") },
            x, y, z,
            u, v,
            width, height,
            textureWidth, textureHeight
        )
    }

}
