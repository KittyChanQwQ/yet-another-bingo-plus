package me.jfenn.bingo.common.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import me.jfenn.bingo.common.Bingo
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.state.GameState
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

object ChatCommand {

    fun sendTeamMessage(
        player: ServerPlayerEntity,
        team: BingoTeam,
        message: Text,
    ) {
        val server = player.server

        val messageText = Text.empty()
            .append(Text.empty().append("TEAM ").append(player.displayName).formatted(team.formatting))
            .append(Text.literal(": "))
            .append(message)

        // broadcast message to team
        team.broadcastMessage(server, messageText)
    }

    fun init() {
        registerCommands {
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

        // override default chat to only send team-specific messages
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register { message, player, _ ->
            val (_, state) = Bingo.getContext(player.server) ?: return@register true
            if (state.state != GameState.PLAYING) return@register true
            val team = BingoTeam.fromPlayer(player) ?: return@register true

            sendTeamMessage(player, team, message.content)

            false
        }
    }
}