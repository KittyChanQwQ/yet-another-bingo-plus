package me.jfenn.bingo.client

import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text

class BingoButtonWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    message: Text,
    onClick: (ButtonWidget) -> Unit,
) : ButtonWidget(
    x,
    y,
    width,
    height,
    message,
    onClick,
    DEFAULT_NARRATION_SUPPLIER,
)