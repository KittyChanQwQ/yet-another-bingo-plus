package me.jfenn.bingo.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

object ChatCommad : CommandProvider {



    override fun commands() = sequence {
        // send a message to all teams, outside of the team-local chat
        listOf("all", "g", "global")
            .map { LiteralArgumentBuilder.literal<ServerCommandSource?>(it) }
            .forEach { command ->
                command.then(
                    RequiredArgumentBuilder.argument<ServerCommandSource?, String?>(
                        "message",
                        StringArgumentType.greedyString()
                    )
                        .executes { ctx ->
                            val player = ctx.source.player ?: return@executes 0
                            val message = StringArgumentType.getString(ctx, "message")

                            ctx.source.server?.playerManager?.broadcast(
                                Text.empty()
                                    .append(Text.literal("<"))
                                    .append(player.name)
                                    .append(Text.literal("> $message")),
                                false,
                            ) ?: return@executes 0
                            1
                        }
                )
                    .also { yield(it) }
            }
    }
}