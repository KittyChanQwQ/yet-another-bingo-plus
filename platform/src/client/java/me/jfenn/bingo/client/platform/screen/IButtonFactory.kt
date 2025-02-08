package me.jfenn.bingo.client.platform.screen

import me.jfenn.bingo.client.platform.text.IText
import net.minecraft.client.gui.widget.ButtonWidget

interface IButtonFactory {

    fun createButton(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        texture: String?,
        focusedTexture: String?,
        onClick: (ButtonWidget) -> Unit,
        message: IText,
    ) : IButton

}

interface IButton