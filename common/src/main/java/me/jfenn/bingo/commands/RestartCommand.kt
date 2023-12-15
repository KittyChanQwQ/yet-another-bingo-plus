package me.jfenn.bingo.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.server.command.ServerCommandSource

object RestartCommand {
    fun init() = registerCommands {
        // Stop (restart) the server
        LiteralArgumentBuilder.literal<ServerCommandSource>("restart")
            .requirePermission(2)
            .executes { ctx ->
                ctx.source.server.stop(false)
                1
            }
            .also { yield(it) }
    }
}
