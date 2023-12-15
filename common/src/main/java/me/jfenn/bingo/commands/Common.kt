package me.jfenn.bingo.commands

import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import me.jfenn.bingo.common.Bingo
import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.state.GameState
import me.jfenn.bingo.utils.hasPermission
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.ServerCommandSource

fun registerCommands(commands: suspend SequenceScope<LiteralArgumentBuilder<ServerCommandSource>>.() -> Unit) {
    CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, environment ->
        for (command in sequence(commands))
            dispatcher.register(command)
    }
}

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

fun <T: ArgumentBuilder<ServerCommandSource?, T>> T.requirePermission(requiredPermissionLevel: Int = 2): T {
    return this.requires { source ->
        val player = source?.player ?: return@requires false
        player.hasPermission(requiredPermissionLevel)
    }
}
