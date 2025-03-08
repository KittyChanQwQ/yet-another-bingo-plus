package me.jfenn.bingo.client.impl.screen.button

import me.jfenn.bingo.client.impl.draw.DrawService
import me.jfenn.bingo.client.platform.screen.IButton
import me.jfenn.bingo.client.platform.screen.IButtonFactory
import me.jfenn.bingo.client.platform.text.IText
import me.jfenn.bingo.common.utils.EventListener
import me.jfenn.bingo.impl.TextImpl
import me.jfenn.bingo.platform.utils.IEventListener
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ButtonTextures
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.TexturedButtonWidget
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.joml.Vector2i

class ButtonFactory : IButtonFactory {
    override fun createButton(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        texture: String?,
        focusedTexture: String?,
        onClick: (ButtonWidget) -> Unit,
        message: IText
    ): IButton = TexturedButtonWidget(
        x,
        y,
        width,
        height,
        ButtonTextures(
            texture?.let { Identifier(it) },
            focusedTexture?.let { Identifier(it) },
        ),
        onClick,
        message.value,
    ).let { TexturedButtonImpl(it) }

    override fun createNinePatchButton(
        sliceSize: Int,
        textureSize: Vector2i,
        texture: String?,
        focusedTexture: String?,
    ): IButton {
        return NinePatchButton(sliceSize, textureSize, texture, focusedTexture)
    }
}

interface ButtonImpl : IButton {
    val button: ClickableWidget
}

class TexturedButtonImpl(
    override val button: ClickableWidget
) : ButtonImpl {
    override var position: Vector2i
        get() = Vector2i(button.x, button.y)
        set(value) {
            button.x = value.x
            button.y = value.y
        }

    override var size: Vector2i
        get() = Vector2i(button.width, button.height)
        set(value) {
            button.width = value.x
            button.height = value.y
        }

    override var message: IText
        get() = TextImpl(button.message.copy())
        set(value) {
            button.message = value.value
        }

    override val onClick: IEventListener<Unit> = EventListener()
}

class NinePatchButton(
    private val sliceSize: Int,
    private val textureSize: Vector2i,
    texture: String?,
    focusedTexture: String?,
) : ButtonImpl {

    private val textures = ButtonTextures(
        texture?.let { Identifier(it) },
        focusedTexture?.let { Identifier(it) },
    )

    override val button = object : ButtonWidget(
        0, 0, 0, 0,
        Text.empty(),
        { onClick.invoke(Unit) },
        DEFAULT_NARRATION_SUPPLIER
    ) {
        override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
            val service = DrawService(context)
            val identifier = textures[this.isNarratable, this.isSelected]
            service.drawNinePatch(identifier, x, y, width, height, sliceSize, textureSize.x, textureSize.y)
            drawMessage(context, MinecraftClient.getInstance().textRenderer, -1)
        }
    }

    override var position: Vector2i
        get() = Vector2i(button.x, button.y)
        set(value) {
            button.x = value.x
            button.y = value.y
        }

    override var size: Vector2i
        get() = Vector2i(button.width, button.height)
        set(value) {
            button.width = value.x
            button.height = value.y
        }

    override var message: IText
        get() = TextImpl(button.message.copy())
        set(value) {
            button.message = value.value
        }

    override val onClick: IEventListener<Unit> = EventListener()
}
