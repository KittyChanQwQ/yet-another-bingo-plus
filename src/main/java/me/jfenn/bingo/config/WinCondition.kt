package me.jfenn.bingo.config

import me.jfenn.bingo.utils.formatTitle

enum class WinCondition(val numLines: Int) {
    ONE_LINE(1),
    TWO_LINES(2),
    THREE_LINES(3),
    FOUR_LINES(4),
    FULL_CARD(12);

    fun formatName(): String {
        return name.formatTitle()
    }
}