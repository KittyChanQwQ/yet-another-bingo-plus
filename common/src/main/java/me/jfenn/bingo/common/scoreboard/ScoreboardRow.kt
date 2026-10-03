package me.jfenn.bingo.common.scoreboard

import me.jfenn.bingo.platform.text.IText

data class ScoreboardRow(
    val text: IText,
    val score: IText? = null,
)

fun IText.toScoreboardRow() = ScoreboardRow(this)
