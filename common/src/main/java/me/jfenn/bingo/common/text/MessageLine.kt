package me.jfenn.bingo.common.text

import me.jfenn.bingo.platform.text.IText

sealed interface MessageLine {
    data class Text(val text: IText) : MessageLine
    data class Placeholder(val placeholder: String) : MessageLine
}