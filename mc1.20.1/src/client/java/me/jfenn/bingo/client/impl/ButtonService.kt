package me.jfenn.bingo.client.impl

import me.jfenn.bingo.client.platform.IButtonService
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.TexturedButtonWidget
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class ButtonService : IButtonService {
    override fun createButton(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        texture: Identifier?,
        focusedTexture: Identifier?,
        onClick: (ButtonWidget) -> Unit,
        message: Text,
    ): ClickableWidget = LegacyTexturedButton(
        x,
        y,
        width,
        height,
        texture,
        focusedTexture,
        onClick,
        message,
    )
}

class LegacyTexturedButton(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    val normalTexture: Identifier?,
    val focusedTexture: Identifier?,
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
