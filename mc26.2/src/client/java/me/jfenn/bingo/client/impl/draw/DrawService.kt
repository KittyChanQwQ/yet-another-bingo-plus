package me.jfenn.bingo.client.impl.draw

import com.mojang.blaze3d.platform.cursor.CursorTypes
import me.jfenn.bingo.client.impl.NativeImageImpl
import me.jfenn.bingo.client.impl.accessor
import me.jfenn.bingo.client.platform.INativeImage
import me.jfenn.bingo.client.platform.renderer.*
import me.jfenn.bingo.impl.ItemStackFactory
import me.jfenn.bingo.platform.Identifier
import me.jfenn.bingo.platform.item.IItemStack
import me.jfenn.bingo.platform.text.IText
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.world.item.ItemStack
import org.joml.Vector2f
import org.joml.Vector2i
import net.minecraft.resources.Identifier as MinecraftIdentifier

class DrawService(
    override val context: GuiGraphicsExtractor,
) : IDrawService {

    override val textRenderer: Font = Minecraft.getInstance().font
    override val font: IFont = FontImpl(textRenderer)
    override val matrices: IMatrixStack = MatrixStackImpl(context.pose())
    override val window: IWindow = Companion

    override fun setCursor(cursor: CursorType) {
        context.requestCursor(
            when (cursor) {
                CursorType.DEFAULT -> com.mojang.blaze3d.platform.cursor.CursorType.DEFAULT
                CursorType.POINTING_HAND -> CursorTypes.POINTING_HAND
                CursorType.RESIZE_HORIZONTAL -> CursorTypes.RESIZE_EW
                CursorType.RESIZE_VERTICAL -> CursorTypes.RESIZE_NS
                CursorType.RESIZE_ALL -> CursorTypes.RESIZE_ALL
            }
        )
    }

    override var delta: Float = 0.5f
    override val mouse: Vector2i
        get() {
            val client = Minecraft.getInstance()
            val mouse = client.mouseHandler
            return Vector2i(
                mouse.getScaledXPos(client.window).toInt(),
                mouse.getScaledYPos(client.window).toInt(),
            )
        }

    companion object : IDrawServiceFactory, IWindow {
        override val window: IWindow = this

        override val scaleFactor: Double get() = Minecraft.getInstance().window.guiScale.toDouble()
        override val scaledWindowWidth: Int
            get() = Minecraft.getInstance().window.guiScaledWidth
        override val scaledWindowHeight: Int
            get() = Minecraft.getInstance().window.guiScaledHeight

        override fun create(drawContext: GuiGraphicsExtractor): IDrawService = DrawService(drawContext)

        /*private val bufferBuilderStorage by lazy {
            BufferBuilderStorage(Runtime.getRuntime().availableProcessors())
        }*/

        /*private val guiRenderState by lazy {
            GuiRenderState()
        }*/

        override val isBufferSupported: Boolean = false

        override fun newBuffer(width: Int, height: Int): IFramebuffer {
            /*val client = MinecraftClient.getInstance()
            val buffer = SimpleFramebuffer(null, width, height, true)
            val context = DrawContext(client, guiRenderState)

            return FramebufferImpl(
                buffer, DrawService(context), guiRenderState
            )*/
            return FramebufferImplStub(width, height)
        }
    }

    override fun draw() {}

    override fun fill(x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
        context.fill(x1, y1, x2, y2, color)
    }

    override fun overlayFill(x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
        context.fill(x1, y1, x2, y2, color)
    }

    override fun drawHorizontalLine(x1: Int, x2: Int, y: Int, color: Int) {
        context.horizontalLine(x1, x2, y, color)
    }

    override fun drawVerticalLine(x: Int, y1: Int, y2: Int, color: Int) {
        context.verticalLine(x, y1, y2, color)
    }

    override fun enableBlend() {}

    override fun disableBlend() {}

    override fun setShaderColor(r: Float, g: Float, b: Float, a: Float) {
        // context.draw()
        // TODO: RenderSystem.setShaderColor(r, g, b, a)
    }

    override fun drawText(text: IText, x: Int, y: Int, color: Int, shadow: Boolean) {
        context.text(textRenderer, text.value, x, y, color, shadow)
    }

    override fun drawItemStack(stack: IItemStack, x: Int, y: Int, seed: Int) {
        require(stack is ItemStackFactory.ItemStackImpl)
        val itemStack: ItemStack = stack.stack
        context.item(itemStack, x, y, seed)

        // draws the item count + durability bar
        val textRenderer = Minecraft.getInstance().font
        context.itemDecorations(textRenderer, stack.stack, x, y)
    }

    override fun drawTooltip(text: List<IText>, x: Int, y: Int) {
        val textRenderer = Minecraft.getInstance().font
        val textList = text.map { it.value.visualOrderText }.toMutableList()
        val realPosition = context.pose().transformPosition(x.toFloat(), y.toFloat(), Vector2f())
        context.setTooltipForNextFrame(textRenderer, textList, realPosition.x.toInt(), realPosition.y.toInt())
    }

    override fun drawTooltipAddon(callback: () -> Unit) {
        val tooltipRunnable = context.accessor.tooltipDrawer
        context.accessor.tooltipDrawer = Runnable {
            tooltipRunnable?.run()
            callback()
        }
    }

    override fun drawTooltipImmediate() {}

    override fun drawDynamicTexture(texture: INativeImage, x: Int, y: Int, width: Int, height: Int) {
        require(texture is NativeImageImpl)
        context.accessor.invokeDrawTexturedQuad(
            RenderPipelines.GUI_TEXTURED,
            texture.glTextureView,
            texture.sampler,
            x, y,
            x + width, y + height,
            0f, 1f,
            0f, 1f,
            -1
        )
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
        context.blit(
            RenderPipelines.GUI_TEXTURED,
            texture.let { MinecraftIdentifier.fromNamespaceAndPath(it.namespace, "textures/gui/sprites/${it.path}.png") },
            x, y,
            u, v,
            width, height,
            textureWidth, textureHeight,
        )
    }

}