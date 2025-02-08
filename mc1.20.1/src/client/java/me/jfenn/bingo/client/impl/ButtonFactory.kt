package me.jfenn.bingo.client.impl

import me.jfenn.bingo.client.platform.screen.IButton
import me.jfenn.bingo.client.platform.screen.IButtonFactory
import me.jfenn.bingo.client.platform.text.IText
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.TexturedButtonWidget
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class ButtonFactory : IButtonFactory {
    override fun createButton(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        texture: String?,
        focusedTexture: String?,
        onClick: (ButtonWidget) -> Unit,
        message: IText,
    ): IButton = LegacyTexturedButton(
        x,
        y,
        width,
        height,
        texture?.let { Identifier(it) },
        focusedTexture?.let { Identifier(it) },
        onClick,
        message.value,
    ).let { ButtonImpl(it) }
}

class ButtonImpl(
    val button: ClickableWidget
) : IButton

class LegacyTexturedButton(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val normalTexture: Identifier?,
    private val focusedTexture: Identifier?,
    onClick: (ButtonWidget) -> Unit,
    message: Text,
) : TexturedButtonWidget(
    x,
    y,
    width,
    height,
    0,
    0,
    0,
    null,
    width,
    height,
    onClick,
    message,
) {
    override fun renderButton(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        val id = if (isSelected) focusedTexture else normalTexture
        context?.drawTexture(
            id?.let { Identifier.of(id.namespace, "textures/gui/sprites/${id.path}.png") },
            x,
            y,
            u.toFloat(),
            v.toFloat(),
            width,
            height,
            textureWidth,
            textureHeight,
        )
    }
}
