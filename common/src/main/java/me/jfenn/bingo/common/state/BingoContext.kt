package me.jfenn.bingo.common.state

import net.minecraft.server.MinecraftServer

data class BingoContext(
    val server: MinecraftServer,
    val state: BingoState,
)
