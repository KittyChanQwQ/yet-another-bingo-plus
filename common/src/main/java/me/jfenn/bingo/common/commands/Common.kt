package me.jfenn.bingo.common.commands

import com.mojang.brigadier.builder.ArgumentBuilder
import me.jfenn.bingo.common.PermissionKey
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.integrations.PermissionsIntegration
import me.jfenn.bingo.platform.commands.IExecutionContext
import me.jfenn.bingo.platform.commands.IExecutionSource
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting

typealias CommandRequirement = (ServerCommandSource) -> Boolean

fun <T: ArgumentBuilder<ServerCommandSource?, T>> T.require(vararg requirements: CommandRequirement): T {
    return this.requires { source ->
        if (source == null) return@requires false
        requirements.all { it(source) }
    }
}

fun IExecutionSource.hasState(vararg requiredState: GameState): Boolean {
    val state = scope.get<BingoState>()
    return requiredState.contains(state.state)
}

fun IExecutionSource.hasPermission(key: PermissionKey): Boolean {
    if (isConsole) return true
    val player = player ?: return false
    val permissions = scope.get<PermissionsIntegration>()
    return permissions.hasPermission(player, key)
}

fun IExecutionSource.hasLobby(): Boolean {
    return scope.get<BingoState>().isLobbyMode
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
