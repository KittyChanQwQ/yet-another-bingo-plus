package me.jfenn.bingo.client

import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TexturedButtonWidget
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class BingoButtonWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    message: Text,
    onClick: (ButtonWidget) -> Unit,
) : TexturedButtonWidget(
    x,
    y,
    width,
    height,
    0,
    0,
    50,
    Identifier.of("bingo", "textures/gui/button.png"),
    80,
    150,
    onClick,
    message,
)