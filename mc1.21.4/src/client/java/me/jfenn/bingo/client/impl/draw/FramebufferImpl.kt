package me.jfenn.bingo.client.impl.draw

import com.mojang.blaze3d.systems.ProjectionType
import com.mojang.blaze3d.systems.RenderSystem
import me.jfenn.bingo.client.platform.renderer.IDrawService
import me.jfenn.bingo.client.platform.renderer.IFramebuffer
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.mixinhelper.FramebufferOverride
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.DiffuseLighting
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.util.Identifier
import org.joml.Matrix4f
import java.util.*
import kotlin.math.roundToInt

class FramebufferImpl(
    private val fb: SimpleFramebuffer,
    private val vertexConsumerProvider: VertexConsumerProvider.Immediate,
    private val service: DrawService,
) : IFramebuffer {

    private val client = MinecraftClient.getInstance()
    private val fbTexture = FramebufferTexture(fb)
    private val fbTextureId = Identifier.of(MOD_ID_BINGO, "framebuffer/" + UUID.randomUUID().toString())

    override fun resize(width: Int, height: Int) {
        val window = client.window
        val textureWidth = (width.toFloat() * window.scaleFactor).roundToInt()
        val textureHeight = (height.toFloat() * window.scaleFactor).roundToInt()
        if (textureWidth != fb.textureWidth || textureHeight != fb.textureHeight) {
            fb.resize(textureWidth, textureHeight)
            client.textureManager.registerTexture(fbTextureId, fbTexture)
        }
    }

    private var backupProjMat = Matrix4f()
    private var backupProjType = ProjectionType.ORTHOGRAPHIC

    override fun beginWrite(): IDrawService {
        fb.setClearColor(0f, 1f, 0f, 1f)
        fb.clear()

        val window = MinecraftClient.getInstance().window

        val right = (fb.textureWidth.toDouble() / window.scaleFactor).toFloat()
        val bottom = (fb.textureHeight.toDouble() / window.scaleFactor).toFloat()
        val matrix4f = Matrix4f().setOrtho(
            0f,
            right,
            bottom,
            0f,
            1000f,
            21000f
        )

        backupProjMat = RenderSystem.getProjectionMatrix()
        backupProjType = RenderSystem.getProjectionType()
        RenderSystem.setProjectionMatrix(matrix4f, ProjectionType.ORTHOGRAPHIC)

        RenderSystem.getModelViewStack()
            .pushMatrix()
            .translation(0f, 0f, -11000.0f)

        DiffuseLighting.enableGuiDepthLighting()

        fb.beginWrite(true)
        FramebufferOverride.framebuffer = fb

        val context: DrawContext = service.context
        service.matrices.push()
        // service.matrices.scale(1f/window.scaleFactor.toFloat(), 1f/window.scaleFactor.toFloat(), 1f)
        // service.matrices.translate(0f, bottom * 0.5f * window.scaleFactor.toFloat(), 0f)

        return service
    }

    override fun endWrite() {
        service.context.draw()
        fb.endWrite()

        service.matrices.pop()

        FramebufferOverride.framebuffer = null
        client.framebuffer.beginWrite(true)

        RenderSystem.getModelViewStack().popMatrix()
        RenderSystem.setProjectionMatrix(backupProjMat, backupProjType)
    }

    override fun draw(service: IDrawService, width: Int, height: Int) {
        val context: DrawContext = service.context
        context.matrices.push()

        val window = client.window
        context.matrices.scale(1f/window.scaleFactor.toFloat(), 1f/window.scaleFactor.toFloat(), 1f)

        val matrix4f = context.matrices.peek().positionMatrix
        val vertexConsumer = context.vertexConsumers.getBuffer(RenderLayer.getGuiTextured(fbTextureId))

        // The framebuffer renders upside down, so we need to flip v1 and v2 here
        // otherwise, this could just be a context.drawTexture call...
        val x1 = 0f
        val x2 = (width * window.scaleFactor).toFloat()
        val y1 = 0f
        val y2 = (height * window.scaleFactor).toFloat()
        val u1 = 0f
        val u2 = 1f
        val v1 = 1f
        val v2 = 0f
        val color = -1

        vertexConsumer.vertex(matrix4f, x1, y1, 0.0f).texture(u1, v1).color(color)
        vertexConsumer.vertex(matrix4f, x1, y2, 0.0f).texture(u1, v2).color(color)
        vertexConsumer.vertex(matrix4f, x2, y2, 0.0f).texture(u2, v2).color(color)
        vertexConsumer.vertex(matrix4f, x2, y1, 0.0f).texture(u2, v1).color(color)

        context.matrices.pop()
    }

    override fun close() {
        fb.delete()
        client.textureManager.destroyTexture(fbTextureId)
    }
}