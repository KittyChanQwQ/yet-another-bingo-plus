package me.jfenn.bingo.common.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import me.jfenn.bingo.common.Permission
import net.minecraft.server.command.ServerCommandSource

class RestartCommand : BaseCommand() {
    init {
        // Stop (restart) the server
        // Only on dedicated server instances
        LiteralArgumentBuilder.literal<ServerCommandSource>("restart")
            .require(
                requireBingoInstance(),
                requirePermission(Permission.COMMAND_RESTART),
                { it.server.isDedicated },
            )
            .executes { ctx ->
                ctx.source.server.stop(false)
                1
            }
            .register()
    }
}
