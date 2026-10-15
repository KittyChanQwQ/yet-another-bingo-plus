package me.jfenn.bingo.common.options

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class LocatorBarMode {
    @SerialName("all")
    ALL,

    @SerialName("team_and_winner")
    TEAM_AND_WINNER,

    @SerialName("team_only")
    TEAM_ONLY;
}
