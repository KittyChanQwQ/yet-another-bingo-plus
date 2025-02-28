package me.jfenn.bingo.client.platform.renderer

import me.jfenn.bingo.client.platform.INativeImage
import me.jfenn.bingo.client.platform.text.IText
import me.jfenn.bingo.platform.item.IItemStack
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.Identifier
import java.io.Closeable
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

interface IDrawService {
    val context: DrawContext
    val textRenderer: TextRenderer
    val font: IFont
    val matrices: IMatrixStack

    fun enableBlend()
    fun disableBlend()

    fun setShaderColor(r: Float, g: Float, b: Float, a: Float)

    fun drawText(text: IText, x: Int, y: Int, color: Int, shadow: Boolean = false)
    fun drawItemStack(stack: IItemStack, x: Int, y: Int, seed: Int)

    fun drawDynamicTexture(
        texture: INativeImage,
        x: Int, y: Int,
        width: Int, height: Int
    )

    fun drawGuiTexture(
        texture: Identifier,
        x: Int, y: Int, z: Int = 0,
        u: Float, v: Float,
        width: Int, height: Int,
        textureWidth: Int, textureHeight: Int,
    )

    fun drawNinePatch(
        texture: Identifier,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        sliceSize: Int,
        textureWidth: Int,
        textureHeight: Int,
    ) {
        val textureCenterWidth = textureWidth - sliceSize*2
        val textureCenterHeight = textureHeight - sliceSize*2
        val centerWidth = width - sliceSize*2
        val centerHeight = height - sliceSize*2
        val xParts = ceil(centerWidth.toFloat() / textureCenterWidth).roundToInt()
        val yParts = ceil(centerHeight.toFloat() / textureCenterHeight).roundToInt()

        // Draw center
        for (xPart in 0 until xParts) {
            val partWidth = min(centerWidth - (xPart * textureCenterWidth), textureCenterWidth)
            for (yPart in 0 until yParts) {
                val partHeight = min(centerHeight - (yPart * textureCenterHeight), textureCenterHeight)
                drawGuiTexture(
                    texture,
                    x = x + sliceSize + xPart*textureCenterWidth, y = y + sliceSize + yPart*textureCenterHeight,
                    u = sliceSize.toFloat(), v = sliceSize.toFloat(),
                    width = partWidth, height = partHeight,
                    textureWidth = textureWidth, textureHeight = textureHeight,
                )
            }
        }

        // Draw horizontal borders
        for (xPart in 0 until xParts) {
            val xPartWidth = min(centerWidth - (xPart * textureCenterWidth), textureCenterWidth)
            drawGuiTexture(
                texture,
                x = x + sliceSize + xPart*textureCenterWidth, y = y,
                u = sliceSize.toFloat(), v = 0f,
                width = xPartWidth, height = sliceSize,
                textureWidth = textureWidth, textureHeight = textureHeight,
            )
            drawGuiTexture(
                texture,
                x = x + sliceSize + xPart*textureCenterWidth, y = y + height - sliceSize,
                u = sliceSize.toFloat(), v = textureHeight.toFloat() - sliceSize,
                width = xPartWidth, height = sliceSize,
                textureWidth = textureWidth, textureHeight = textureHeight,
            )
        }

        // Draw vertical borders
        for (yPart in 0 until yParts) {
            val yPartHeight = min(centerHeight - (yPart * textureCenterHeight), textureCenterHeight)
            drawGuiTexture(
                texture,
                x = x, y = y + sliceSize + yPart*textureCenterHeight,
                u = 0f, v = sliceSize.toFloat(),
                width = sliceSize, height = yPartHeight,
                textureWidth = textureWidth, textureHeight = textureHeight,
            )
            drawGuiTexture(
                texture,
                x = x + width - sliceSize, y = y + sliceSize + yPart*textureCenterHeight,
                u = textureWidth.toFloat() - sliceSize, v = sliceSize.toFloat(),
                width = sliceSize, height = yPartHeight,
                textureWidth = textureWidth, textureHeight = textureHeight,
            )
        }

        // Draw corners
        drawGuiTexture(
            texture,
            x = x, y = y,
            u = 0f, v = 0f,
            width = sliceSize, height = sliceSize,
            textureWidth = textureWidth, textureHeight = textureHeight,
        )
        drawGuiTexture(
            texture,
            x = x + width - sliceSize, y = y,
            u = textureWidth.toFloat() - sliceSize, v = 0f,
            width = sliceSize, height = sliceSize,
            textureWidth = textureWidth, textureHeight = textureHeight,
        )
        drawGuiTexture(
            texture,
            x = x, y = y + height - sliceSize,
            u = 0f, v = textureHeight.toFloat() - sliceSize,
            width = sliceSize, height = sliceSize,
            textureWidth = textureWidth, textureHeight = textureHeight,
        )
        drawGuiTexture(
            texture,
            x = x + width - sliceSize, y = y + height - sliceSize,
            u = textureWidth.toFloat() - sliceSize, v = textureHeight.toFloat() - sliceSize,
            width = sliceSize, height = sliceSize,
            textureWidth = textureWidth, textureHeight = textureHeight,
        )
    }
}

interface IDrawServiceFactory {
    fun create(drawContext: DrawContext): IDrawService

    fun newBuffer(width: Int, height: Int): IFramebuffer
}

interface IFramebuffer : Closeable {
    val width: Int
    val height: Int
    fun register()
    fun resize(width: Int, height: Int)
    fun beginWrite(): IDrawService
    fun endWrite()
    fun draw(service: IDrawService, width: Int, height: Int)
}
