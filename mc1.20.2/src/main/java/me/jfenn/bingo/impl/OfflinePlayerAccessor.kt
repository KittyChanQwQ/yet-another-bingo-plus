package me.jfenn.bingo.impl

import com.mojang.authlib.GameProfile
import me.jfenn.bingo.api.IOfflinePlayerAccessor
import me.jfenn.bingo.common.card.BingoPlayerProfile
import net.minecraft.network.packet.c2s.common.SyncedClientOptions
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import java.util.*

class OfflinePlayerAccessor : IOfflinePlayerAccessor {
    override fun getOfflinePlayer(server: MinecraftServer, profile: BingoPlayerProfile): ServerPlayerEntity {
        val player = ServerPlayerEntity(
            server,
            server.overworld,
            GameProfile(UUID.fromString(profile.uuid), profile.name),
            SyncedClientOptions.createDefault(),
        )

        server.playerManager.loadPlayerData(player)
        return player
    }
}