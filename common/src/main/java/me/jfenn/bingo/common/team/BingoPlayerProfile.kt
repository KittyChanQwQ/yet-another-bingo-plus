package me.jfenn.bingo.common.team

import kotlinx.serialization.Serializable
import me.jfenn.bingo.api.IPlayerHandle
import me.jfenn.bingo.common.utils.UuidAsString

@Serializable
data class BingoPlayerProfile(
    val uuid: UuidAsString,
    val name: String,
) {
    companion object {
        fun fromPlayer(playerEntity: IPlayerHandle): BingoPlayerProfile {
            return BingoPlayerProfile(
                uuid = playerEntity.uuid,
                name = playerEntity.name,
            )
        }
    }
}
