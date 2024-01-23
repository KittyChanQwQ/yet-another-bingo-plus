package me.jfenn.bingo.common.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.server.command.ServerCommandSource

class RestartCommand : BaseCommand() {
    init {
        // Stop (restart) the server
        // Only on dedicated server instances
        LiteralArgumentBuilder.literal<ServerCommandSource>("restart")
            .requireBingoInstance()
            .requirePermission(permissions::canUseRestartCommand)
            .requires { it.server.isDedicated }
            .executes { ctx ->
                ctx.source.server.stop(false)
                1
            }
            .register()
    }
}
