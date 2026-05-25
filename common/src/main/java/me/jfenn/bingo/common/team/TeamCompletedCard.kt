package me.jfenn.bingo.common.team

import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.scoring.StalemateReason
import me.jfenn.bingo.common.utils.InstantType

@Serializable
internal data class TeamCompletedCard(
    val card: BingoCard,
    val completedAt: InstantType,
    /**
     * True if the team completed the card & should be counted in the score
     * False if the card was skipped or completed by another team
     */
    val isWinner: Boolean,
    /**
     * If the team won the card automatically because of a stalemate
     */
    val stalemate: StalemateReason?,
    val score: TeamScore,
) {
    val isAutoWin get() = stalemate is StalemateReason.WinByStalemate
}