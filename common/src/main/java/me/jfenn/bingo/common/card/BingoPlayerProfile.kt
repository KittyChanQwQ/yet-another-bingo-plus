package me.jfenn.bingo.common.card

import kotlinx.serialization.Serializable
import net.minecraft.server.network.ServerPlayerEntity

@Serializable
data class BingoPlayerProfile(
    val uuid: String,
    val name: String,
) {
    companion object {
        fun fromPlayer(playerEntity: ServerPlayerEntity): BingoPlayerProfile {
            return BingoPlayerProfile(
                uuid = playerEntity.uuidAsString,
                name = playerEntity.gameProfile.name,
            )
        }
    }
}
