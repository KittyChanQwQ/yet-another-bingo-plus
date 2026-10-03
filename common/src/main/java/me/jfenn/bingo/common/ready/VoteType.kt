package me.jfenn.bingo.common.ready

import kotlinx.serialization.Serializable
import me.jfenn.bingo.generated.StringKey
import java.util.*

@Serializable
enum class VoteType(val id: UUID) {
    /**
     * Eligible in PREGAME only - starts the round
     */
    START(UUID.randomUUID()),

    /**
     * Eligible in POSTGAME - starts a new round
     */
    NEXT_ROUND(UUID.randomUUID()),

    /**
     * Eligible in POSTGAME - returns to the lobby
     */
    RETURN_TO_LOBBY(UUID.randomUUID()),

    /**
     * Eligible in POSTGAME - resumes the current game with extended goal/time limit
     */
    KEEP_PLAYING(UUID.randomUUID());

    val timeRemainingKey get() = when (this) {
        START -> StringKey.LobbyVoteStartingTimeRemaining
        NEXT_ROUND -> StringKey.LobbyVoteNextRoundTimeRemaining
        RETURN_TO_LOBBY -> StringKey.LobbyVoteLeavingTimeRemaining
        KEEP_PLAYING -> StringKey.LobbyVoteResumingTimeRemaining
    }
}