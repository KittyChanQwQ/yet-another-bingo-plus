package me.jfenn.bingo.common.utils

import net.minecraft.text.MutableText
import net.minecraft.text.Text

fun List<Text>.joinText(separator: Text = Text.literal(", ")): MutableText {
    val t = Text.empty()
    forEachIndexed { index, text ->
        t.append(text)
        if (index < size-1) t.append(separator)
    }
    return t
}
