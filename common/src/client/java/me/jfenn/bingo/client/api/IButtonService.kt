package me.jfenn.bingo.client.api

import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text
import net.minecraft.util.Identifier

interface IButtonService {

    fun createButton(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        texture: Identifier?,
        focusedTexture: Identifier?,
        onClick: (ButtonWidget) -> Unit,
        message: Text,
    ) : ClickableWidget

}