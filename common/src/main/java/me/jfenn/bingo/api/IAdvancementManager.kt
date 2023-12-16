package me.jfenn.bingo.api

import net.minecraft.server.network.ServerPlayerEntity

interface IAdvancementManager {

    fun clearAdvancements(player: ServerPlayerEntity)

}