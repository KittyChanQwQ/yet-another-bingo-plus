package me.jfenn.bingo.client.impl

import me.jfenn.bingo.client.platform.screen.IButton
import me.jfenn.bingo.client.platform.screen.IButtonFactory
import me.jfenn.bingo.client.platform.text.IText
import net.minecraft.client.gui.screen.ButtonTextures
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.TexturedButtonWidget
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
        message: IText
    ): IButton = TexturedButtonWidget(
        x,
        y,
        width,
        height,
        ButtonTextures(
            texture?.let { Identifier.of(it) },
            focusedTexture?.let { Identifier.of(it) },
        ),
        onClick,
        message.value,
    ).let { ButtonImpl(it) }
}

class ButtonImpl(
    val button: ClickableWidget
) : IButton
