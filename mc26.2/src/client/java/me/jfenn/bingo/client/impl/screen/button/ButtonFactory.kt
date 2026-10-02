package me.jfenn.bingo.client.impl.screen.button

import me.jfenn.bingo.client.impl.draw.DrawService
import me.jfenn.bingo.client.platform.screen.IButton
import me.jfenn.bingo.client.platform.screen.IButtonFactory
import me.jfenn.bingo.common.utils.EventListener
import me.jfenn.bingo.impl.TextImpl
import me.jfenn.bingo.impl.toMinecraftIdentifier
import me.jfenn.bingo.impl.toPlatformIdentifier
import me.jfenn.bingo.platform.text.IText
import me.jfenn.bingo.platform.utils.IEventListener
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.InputWithModifiers
import net.minecraft.network.chat.Component
import org.joml.Vector2i

class ButtonFactory : IButtonFactory {
    override fun createDefaultButton(
        message: IText,
        onClick: () -> Unit
    ) = Button.builder(message.value, { onClick() })
        .build()
        .let { TexturedButtonImpl(it) }

    override fun createButton(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        textures: IButtonFactory.ButtonTextures,
        onClick: (Button) -> Unit,
        message: IText
    ): IButton = ImageButton(
        x,
        y,
        width,
        height,
        textures.toWidgetSprites(),
        onClick,
        message.value,
    ).let { TexturedButtonImpl(it) }

    override fun createNinePatchButton(
        sliceSize: Int,
        textureSize: Vector2i,
        textures: IButtonFactory.ButtonTextures,
    ): IButton {
        return NinePatchButton(sliceSize, textureSize, textures.toWidgetSprites())
    }
}

interface ButtonImpl : IButton {
    val button: AbstractWidget
}

class TexturedButtonImpl(
    override val button: AbstractWidget
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

    override var active: Boolean by button::active

    override val onClick: IEventListener<Unit> = EventListener()
}

class NinePatchButton(
    private val sliceSize: Int,
    private val textureSize: Vector2i,
    private val textures: WidgetSprites,
) : ButtonImpl {

    override val button = object : AbstractButton(
        0, 0, 0, 0,
        Component.empty(),
    ) {
        override fun extractContents(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
            val service = DrawService(graphics)
            val identifier = textures[this.active, this.isHoveredOrFocused]
            service.drawNinePatch(identifier.toPlatformIdentifier(), x, y, width, height, sliceSize, textureSize.x, textureSize.y)
            extractScrollingStringOverContents(graphics.textRenderer(), message, 2)
        }

        override fun updateWidgetNarration(output: NarrationElementOutput) {
            defaultButtonNarrationText(output)
        }

        override fun onPress(input: InputWithModifiers) {
            onClick.invoke(Unit)
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

    override var active: Boolean by button::active

    override val onClick: IEventListener<Unit> = EventListener()
}

private fun IButtonFactory.ButtonTextures.toWidgetSprites() = WidgetSprites(
    texture.toMinecraftIdentifier(),
    inactiveTexture.toMinecraftIdentifier(),
    focusedTexture.toMinecraftIdentifier(),
    inactiveTexture.toMinecraftIdentifier(),
)
