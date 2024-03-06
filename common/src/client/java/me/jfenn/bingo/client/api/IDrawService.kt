package me.jfenn.bingo.client.api

import net.minecraft.client.gui.DrawContext
import net.minecraft.util.Identifier
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

interface IDrawService {
    fun drawTexture(
        texture: Identifier,
        x: Int, y: Int,
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
                drawTexture(
                    texture,
                    x + sliceSize + xPart*textureCenterWidth, y + sliceSize + yPart*textureCenterHeight,
                    sliceSize.toFloat(), sliceSize.toFloat(),
                    partWidth, partHeight,
                    textureWidth, textureHeight,
                )
            }
        }

        // Draw horizontal borders
        for (xPart in 0 until xParts) {
            val xPartWidth = min(centerWidth - (xPart * textureCenterWidth), textureCenterWidth)
            drawTexture(
                texture,
                x + sliceSize + xPart*textureCenterWidth, y,
                sliceSize.toFloat(), 0f,
                xPartWidth, sliceSize,
                textureWidth, textureHeight
            )
            drawTexture(
                texture,
                x + sliceSize + xPart*textureCenterWidth, y + height - sliceSize,
                sliceSize.toFloat(), textureHeight.toFloat() - sliceSize,
                xPartWidth, sliceSize,
                textureWidth, textureHeight
            )
        }

        // Draw vertical borders
        for (yPart in 0 until yParts) {
            val yPartHeight = min(centerHeight - (yPart * textureCenterHeight), textureCenterHeight)
            drawTexture(
                texture,
                x, y + sliceSize + yPart*textureCenterHeight,
                0f, sliceSize.toFloat(),
                sliceSize, yPartHeight,
                textureWidth, textureHeight
            )
            drawTexture(
                texture,
                x + width - sliceSize, y + sliceSize + yPart*textureCenterHeight,
                textureWidth.toFloat() - sliceSize, sliceSize.toFloat(),
                sliceSize, yPartHeight,
                textureWidth, textureHeight
            )
        }

        // Draw corners
        drawTexture(
            texture,
            x, y,
            0f, 0f,
            sliceSize, sliceSize,
            textureWidth, textureHeight
        )
        drawTexture(
            texture,
            x + width - sliceSize, y,
            textureWidth.toFloat() - sliceSize, 0f,
            sliceSize, sliceSize,
            textureWidth, textureHeight
        )
        drawTexture(
            texture,
            x, y + height - sliceSize,
            0f, textureHeight.toFloat() - sliceSize,
            sliceSize, sliceSize,
            textureWidth, textureHeight
        )
        drawTexture(
            texture,
            x + width - sliceSize, y + height - sliceSize,
            textureWidth.toFloat() - sliceSize, textureHeight.toFloat() - sliceSize,
            sliceSize, sliceSize,
            textureWidth, textureHeight
        )
    }
}

interface IDrawServiceFactory {
    fun create(drawContext: DrawContext): IDrawService
}
