package me.jfenn.bingo.config

enum class WinCondition(val numLines: Int) {
    ONE_LINE(1),
    TWO_LINES(2),
    THREE_LINES(3),
    FULL_CARD(12);
}