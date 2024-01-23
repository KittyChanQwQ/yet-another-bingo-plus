package me.jfenn.bingo.common.commands

import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import me.jfenn.bingo.common.scope.BingoKoin
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import org.koin.core.scope.Scope

typealias CommandRequirement = (ServerCommandSource) -> Boolean

fun requireBingoInstance(): CommandRequirement = {
    BingoKoin.getScope(it.server) != null
}

fun requireState(vararg requiredState: GameState): CommandRequirement = { source ->
    val scope = BingoKoin.getScope(source.server)

    if (scope != null) {
        val state = scope.get<BingoState>()
        requiredState.contains(state.state)
    } else {
        false
    }
}

fun requirePermission(getter: (ServerPlayerEntity) -> Boolean): CommandRequirement = { source ->
    val player = source.player
    player != null && getter(player)
}

fun <T: ArgumentBuilder<ServerCommandSource?, T>> T.require(vararg requirements: CommandRequirement): T {
    return this.requires { source ->
        if (source == null) return@requires false
        requirements.all { it(source) }
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
