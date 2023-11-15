package me.jfenn.bingo.commands

import com.mojang.brigadier.builder.ArgumentBuilder
import me.jfenn.bingo.Bingo
import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.state.GameState
import net.minecraft.server.command.ServerCommandSource

fun <T: ArgumentBuilder<ServerCommandSource?, T>> T.requireState(requiredState: GameState): T {
    return this.requires { source ->
        if (source == null) return@requires false
        val ctx = Bingo.getContext(source.server) ?: return@requires false
        ctx.state.state == requiredState
    }
}

fun <T: ArgumentBuilder<ServerCommandSource?, T>> T.requireBingoTeam(): T {
    return this.requires { source ->
        val player = source?.player ?: return@requires false
        BingoTeam.fromPlayer(player) != null
    }
}
