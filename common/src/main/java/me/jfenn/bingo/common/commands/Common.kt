package me.jfenn.bingo.common.commands

import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import me.jfenn.bingo.common.scope.BingoKoin
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.utils.hasPermission
import net.minecraft.server.command.ServerCommandSource
import org.koin.core.scope.Scope

fun <T: ArgumentBuilder<ServerCommandSource?, T>> T.requireBingoInstance(): T {
    return this.requires { source ->
        if (source == null) return@requires false
        BingoKoin.getScope(source.server) != null
    }
}

fun <T: ArgumentBuilder<ServerCommandSource?, T>> T.requireState(vararg requiredState: GameState): T {
    return this.requires { source ->
        val scope = source?.server
            ?.let { BingoKoin.getScope(it) }
            ?: return@requires false

        val state = scope.get<BingoState>()
        requiredState.contains(state.state)
    }
}

fun <T: ArgumentBuilder<ServerCommandSource?, T>> T.requireBingoTeam(): T {
    return this.requires { source ->
        val player = source?.player ?: return@requires false
        BingoTeam.fromPlayer(player) != null
    }
}

fun <T: ArgumentBuilder<ServerCommandSource?, T>> T.requirePermission(requiredPermissionLevel: Int = 2): T {
    return this.requires { source ->
        val player = source?.player ?: return@requires false
        player.hasPermission(requiredPermissionLevel)
    }
}

fun <T: ArgumentBuilder<ServerCommandSource, T>> T.executesInScope(callback: Scope.(CommandContext<ServerCommandSource>) -> Int): T {
    return this.executes { ctx ->
        val scope = ctx.source?.server
            ?.let { BingoKoin.getScope(it) }
            ?: return@executes 0

        callback.invoke(scope, ctx)
    }
}
