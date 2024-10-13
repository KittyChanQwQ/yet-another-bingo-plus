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

fun Text.wrap(wrapLength: Int): List<Text> {
    val str = this.string
    return if (str.length > wrapLength) {
        val words = str.split(Regex("\\s"))
        val ret = mutableListOf<String>()
        val current = StringBuilder()

        for (word in words) {
            if (current.length > wrapLength) {
                ret.add(current.toString().trim())
                current.clear()
            }

            current.append(word)
            current.append(" ")
        }

        if (current.isNotBlank()) {
            ret.add(current.toString().trim())
        }

        ret.map { Text.literal(it).setStyle(this.style) }
    } else {
        listOf(this)
    }
}

fun Text.truncate(maxLength: Int, postfix: String = "..."): MutableText {
    val str = this.string
    return if (str.length > maxLength) {
        Text.literal(str.substring(0, (maxLength - postfix.length).coerceAtLeast(1)))
            .setStyle(this.style)
    } else {
        copy()
    }
}
