package me.jfenn.bingo.api

import me.jfenn.bingo.common.card.BingoPlayerProfile
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity

interface IOfflinePlayerAccessor {

    fun getOfflinePlayer(server: MinecraftServer, profile: BingoPlayerProfile): ServerPlayerEntity

}