package me.jfenn.bingo.common.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.server.command.ServerCommandSource

object RestartCommand {
    fun init() = registerCommands {
        // Stop (restart) the server
        // Only on dedicated server instances
        LiteralArgumentBuilder.literal<ServerCommandSource>("restart")
            .requireBingoInstance()
            .requirePermission(2)
            .requires { it.server.isDedicated }
            .executes { ctx ->
                ctx.source.server.stop(false)
                1
            }
            .also { yield(it) }
    }
}
