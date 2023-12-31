package me.jfenn.bingo.client.impl

import me.jfenn.bingo.client.api.IButtonService
import net.minecraft.client.gui.screen.ButtonTextures
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
        message: Text
    ): ClickableWidget = TexturedButtonWidget(
        x,
        y,
        width,
        height,
        ButtonTextures(
            texture,
            focusedTexture,
        ),
        onClick,
        message,
    )
}