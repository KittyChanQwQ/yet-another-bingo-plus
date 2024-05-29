package me.jfenn.bingo.common.stats.data

import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.utils.UuidAsString
import me.jfenn.bingo.stats.sql.GameTeam
import java.util.*

@Serializable
data class GameTeamInfo(
    val id: String,
    val gameId: UuidAsString,
    val name: String,
    val isWinner: Boolean
) {
    constructor(team: GameTeam) : this(
        id = team.id,
        gameId = UUID.fromString(team.game_id),
        name = team.name,
        isWinner = team.is_winner == 1L,
    )

    fun toGameTeam() = GameTeam(
        id = this.id,
        game_id = this.gameId.toString(),
        name = this.name,
        is_winner = if (this.isWinner) 1L else 0L
    )
}