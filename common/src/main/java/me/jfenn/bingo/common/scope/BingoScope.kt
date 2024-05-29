package me.jfenn.bingo.common.scope

import me.jfenn.bingo.common.state.BingoState
import net.minecraft.server.MinecraftServer

class BingoScope(
    val server: MinecraftServer,
    val state: BingoState,
)