package me.jfenn.bingo.common.state

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class TeamWinner(
    val instant: Instant,
)