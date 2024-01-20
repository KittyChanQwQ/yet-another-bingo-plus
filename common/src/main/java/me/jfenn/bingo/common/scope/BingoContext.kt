package me.jfenn.bingo.common.scope

import me.jfenn.bingo.common.state.BingoState
import net.minecraft.server.MinecraftServer

data class BingoContext(
    val server: MinecraftServer,
    val state: BingoState,
)