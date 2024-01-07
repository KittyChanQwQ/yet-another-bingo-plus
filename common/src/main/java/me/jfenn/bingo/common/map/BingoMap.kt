package me.jfenn.bingo.common.map

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.card.BingoTeam

@Serializable
data class BingoMap(
    val mapId: Int,
    val team: BingoTeam?,
    var updatedAt: Instant? = Clock.System.now(),
) {

    val teamColor = team?.color ?: 22

}
