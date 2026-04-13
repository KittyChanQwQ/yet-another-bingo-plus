package me.jfenn.bingo.platform.scoreboard

import me.jfenn.bingo.platform.text.IText

sealed class ScoreChange {

    abstract val name: String
    abstract val text: IText?
    abstract val value: Int

    data class Create(
        override val name: String,
        override val text: IText,
        override val value: Int,
    ): ScoreChange()

    data class Update(
        override val name: String,
        override val text: IText,
        override val value: Int,
    ): ScoreChange()

    data class Remove(
        override val name: String,
        override val text: IText? = null,
        override val value: Int,
    ): ScoreChange()

}