package me.jfenn.bingo.common.autorestart

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.commands.*
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.sendInfoMessage
import me.jfenn.bingo.generated.StringKey
import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

class RestartCommand : BaseCommand() {
    init {
        if (FabricLoader.getInstance().environmentType ==  EnvType.SERVER) {
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
                .then(
                    LiteralArgumentBuilder.literal<ServerCommandSource>("abort")
                        .executesInScope { ctx ->
                            val text = get<TextProvider>()
                            val timerState = get<RestartTimerState>()

                            if (timerState.shutdownAt != null) {
                                timerState.shutdownAt = null

                                ctx.source.sendFeedback(
                                    { text.string(StringKey.CommandRestartAborted) },
                                    true,
                                )
                            } else {
                                ctx.source.sendFeedback(
                                    { text.string(StringKey.CommandRestartNotScheduled) },
                                    false,
                                )
                            }

                            1
                        }
                )
                .register()
        }
    }
}