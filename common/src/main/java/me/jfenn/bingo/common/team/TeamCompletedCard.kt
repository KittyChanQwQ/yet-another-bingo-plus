package me.jfenn.bingo.common.team

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.utils.UuidAsString

@Serializable
data class TeamCompletedCard(
    val id: UuidAsString,
    val completedAt: Instant,
    val isWinner: Boolean,
    val score: TeamScore,
)