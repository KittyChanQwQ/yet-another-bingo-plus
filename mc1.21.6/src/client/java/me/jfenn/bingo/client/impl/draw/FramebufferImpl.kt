package me.jfenn.bingo.client.impl.draw

import me.jfenn.bingo.client.impl.accessor
import me.jfenn.bingo.client.mixinhelper.FramebufferOverride
import me.jfenn.bingo.client.platform.renderer.IDrawService
import me.jfenn.bingo.client.platform.renderer.IFramebuffer
import me.jfenn.bingo.common.MOD_ID_BINGO
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.util.Identifier
import java.util.*

class FramebufferImpl(
    private val fb: SimpleFramebuffer,
    private val service: DrawService,
    private val renderState: GuiRenderState,
) : IFramebuffer {

    private val client = MinecraftClient.getInstance()
    private val fbTexture = FramebufferTexture(fb)
    private val fbTextureId = Identifier.of(MOD_ID_BINGO, "framebuffer/" + UUID.randomUUID().toString())

    override val width: Int
        get() = fb.textureWidth

    override val height: Int
        get() = fb.textureHeight

    override fun register() {
        client.textureManager.registerTexture(fbTextureId, fbTexture)
    }

    override fun resize(width: Int, height: Int) {
        fb.resize(width, height)
    }

    override fun write(callback: (IDrawService) -> Unit) {
        FramebufferOverride.framebuffer = fb

        renderState.clear()
        service.matrices.push()

        callback(service)

        // context.draw()

        service.matrices.pop()

        FramebufferOverride.framebuffer = null
    }

    override fun draw(service: IDrawService, width: Int, height: Int) {
        val context: DrawContext = service.context
        context.matrices.pushMatrix()

        val scaleFactor = fb.textureWidth.toFloat() / width
        context.matrices.scale(1f/scaleFactor, 1f/scaleFactor)

        // The framebuffer renders upside down, so we need to flip v1 and v2 here
        // otherwise, this could just be a context.drawTexture call...
        val x1 = 0
        val x2 = (width * scaleFactor).toInt()
        val y1 = 0
        val y2 = (height * scaleFactor).toInt()
        val u1 = 0f
        val u2 = 1f
        val v1 = 1f
        val v2 = 0f
        val color = -1

        context.accessor.invokeDrawTexturedQuad(
            RenderPipelines.GUI_TEXTURED,
            fb.colorAttachmentView,
            x1, y1,
            x2, y2,
            u1, u2,
            v1, v2,
            color
        )

        context.matrices.popMatrix()
        // context.draw()
    }

    override fun close() {
        fb.delete()
        client.textureManager.destroyTexture(fbTextureId)
    }
}