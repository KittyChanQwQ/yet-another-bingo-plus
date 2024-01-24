package me.jfenn.bingo.common.scoreboard

sealed class ScoreChange {

    abstract val name: String
    abstract val value: Int

    data class Create(
        override val name: String,
        override val value: Int,
    ): ScoreChange()

    data class Update(
        override val name: String,
        override val value: Int,
    ): ScoreChange()

    data class Remove(
        override val name: String,
        override val value: Int,
    ): ScoreChange()

}