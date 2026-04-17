package me.jfenn.bingo.api.options

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface EndWhen {
    @Serializable
    @SerialName("never")
    data object Never : EndWhen

    @Serializable
    @SerialName("first_win")
    data object FirstWin : EndWhen

    @Serializable
    @SerialName("teams_win")
    data class TeamsWin(val teams: Int) : EndWhen

    @Serializable
    @SerialName("all_win")
    data object AllWin : EndWhen
}