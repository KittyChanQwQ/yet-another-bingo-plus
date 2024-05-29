package me.jfenn.bingo.common.stats.data

import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.utils.UuidAsString
import me.jfenn.bingo.stats.sql.GamePlayer
import java.util.*

@Serializable
data class GamePlayerInfo(
    val teamId: String,
    val gameId: UuidAsString,
    val minecraftId: UuidAsString,
    val minecraftName: String,
    val capturedItems: Int,
) {
    constructor(player: GamePlayer) : this(
        teamId = player.team_id,
        gameId = UUID.fromString(player.game_id),
        minecraftId = UUID.fromString(player.minecraft_id),
        minecraftName = player.minecraft_name,
        capturedItems = player.captured_items.toInt()
    )

    fun toGamePlayer() = GamePlayer(
        team_id = this.teamId,
        game_id = this.gameId.toString(),
        minecraft_id = this.minecraftId.toString(),
        minecraft_name = this.minecraftName,
        captured_items = this.capturedItems.toLong()
    )
}