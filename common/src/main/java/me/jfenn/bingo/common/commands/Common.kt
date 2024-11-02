package me.jfenn.bingo.common.commands

import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.api.commands.IExecutionContext
import me.jfenn.bingo.api.commands.IExecutionSource
import me.jfenn.bingo.common.PermissionKey
import me.jfenn.bingo.common.scope.BingoKoin
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.integrations.PermissionsIntegration
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.koin.core.scope.Scope

typealias CommandRequirement = (ServerCommandSource) -> Boolean

inline fun requireInScope(crossinline req: Scope.(ServerCommandSource) -> Boolean): CommandRequirement = { source ->
    val scope = BingoKoin.getScope(source.server)
    scope != null && req.invoke(scope, source)
}

fun requireBingoInstance(): CommandRequirement = {
    BingoKoin.getScope(it.server) != null
}

fun requireState(vararg requiredState: GameState): CommandRequirement = requireInScope {
    val state = get<BingoState>()
    requiredState.contains(state.state)
}

fun requirePermission(key: PermissionKey): CommandRequirement = requireInScope { source ->
    val player = source.player ?: return@requireInScope false
    val permissions = get<PermissionsIntegration>()
    permissions.hasPermission(get<IPlayerManager>().forPlayer(player), key)
}

fun requirePlaying(): CommandRequirement = requireInScope { source ->
    val player = source.player ?: return@requireInScope false
    val teamService = get<TeamService>()
    teamService.isPlaying(player)
}

fun <T: ArgumentBuilder<ServerCommandSource?, T>> T.require(vararg requirements: CommandRequirement): T {
    return this.requires { source ->
        if (source == null) return@requires false
        requirements.all { it(source) }
    }
}

inline fun <T: ArgumentBuilder<ServerCommandSource, T>> T.executesInScope(
    crossinline callback: Scope.(CommandContext<ServerCommandSource>) -> Int
): T {
    return this.executes { ctx ->
        val scope = ctx.source?.server
            ?.let { BingoKoin.getScope(it) }
            ?: return@executes 0

        callback.invoke(scope, ctx)
    }
}

fun IExecutionSource.hasState(vararg requiredState: GameState): Boolean {
    val state = scope.get<BingoState>()
    return requiredState.contains(state.state)
}

fun IExecutionSource.hasPermission(key: PermissionKey): Boolean {
    val player = player ?: return false
    val permissions = scope.get<PermissionsIntegration>()
    return permissions.hasPermission(player, key)
}

fun IExecutionContext.sendInfoFeedback(text: MutableText) {
    Text.empty()
        .append("ℹ ")
        .append(text.formatted(Formatting.ITALIC))
        .formatted(Formatting.AQUA)
        .also { sendFeedback(it) }
}

fun IExecutionContext.sendInfoMessage(text: MutableText) {
    Text.empty()
        .append("ℹ ")
        .append(text.formatted(Formatting.ITALIC))
        .formatted(Formatting.AQUA)
        .also { sendMessage(it) }
}

fun IExecutionContext.sendWarningMessage(text: MutableText) {
    Text.empty()
        .append("⚠ ")
        .append(text.formatted(Formatting.ITALIC))
        .formatted(Formatting.YELLOW)
        .also { sendMessage(it) }
}
