package me.jfenn.bingo.common.stats

import net.minecraft.server.MinecraftServer

/**
 * Formats a number with commas
 */
fun Long.formatLargeNumber(): String {
    return String.format("%,d", this)
}

val MinecraftServer.isSingleplayerForStatistics get() = isSingleplayer && currentPlayerCount <= 1
