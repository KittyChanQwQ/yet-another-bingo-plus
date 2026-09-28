package me.jfenn.bingo.common.team

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.utils.InstantType
import me.jfenn.bingo.platform.utils.UuidAsString

@Serializable
internal sealed interface TeamState {
    @SerialName("playing")
    data class Playing(val cardId: UuidAsString) : TeamState
    @SerialName("pending_card")
    data class PendingCard(val cardId: UuidAsString?) : TeamState
    @SerialName("finished")
    data class Finished(val completedAt: InstantType) : TeamState
}
