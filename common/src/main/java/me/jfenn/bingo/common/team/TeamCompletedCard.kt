package me.jfenn.bingo.common.team

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.card.BingoCard

@Serializable
data class TeamCompletedCard(
    val card: BingoCard,
    val completedAt: Instant,
    val isWinner: Boolean,
    /**
     * True if the team won the card automatically because of a stalemate
     */
    val isAutoWin: Boolean,
    val score: TeamScore,
)