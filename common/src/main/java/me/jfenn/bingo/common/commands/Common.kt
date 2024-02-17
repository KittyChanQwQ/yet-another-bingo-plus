package me.jfenn.bingo.common.commands

import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import me.jfenn.bingo.common.PermissionKey
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.scope.BingoKoin
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.integrations.PermissionsIntegration
import net.minecraft.server.command.ServerCommandSource
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

fun requirePermission(key: PermissionKey): CommandRequirement = { source ->
    val scope = BingoKoin.getScope(source.server)
    val player = source.player

    if (scope != null && player != null) {
        val permissions = scope.get<PermissionsIntegration>()
        permissions.hasPermission(player, key)
    } else {
        false
    }
}

fun requirePlaying(): CommandRequirement = { source ->
    val scope = BingoKoin.getScope(source.server)
    val player = source.player

    if (scope != null && player != null) {
        val teamService = scope.get<TeamService>()
        teamService.isPlaying(player)
    } else {
        false
    }
}

fun requireNotPlaying(): CommandRequirement = { source ->
    val scope = BingoKoin.getScope(source.server)
    val player = source.player

    if (scope != null && player != null) {
        val teamService = scope.get<TeamService>()
        !teamService.isPlaying(player)
    } else {
        false
    }
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
