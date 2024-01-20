package me.jfenn.bingo.common.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import me.jfenn.bingo.common.scope.BingoKoin
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

class ChatCommand : BaseCommand() {

    companion object {
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
    }

    init {
        // send a message to all teams, outside of the team-local chat
        listOf("all", "g", "global")
            .map { LiteralArgumentBuilder.literal<ServerCommandSource?>(it) }
            .map { it.requireBingoInstance() }
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
                    .register()
            }

        // override default chat to only send team-specific messages
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register { message, player, _ ->
            val scope = BingoKoin.getScope(player.server) ?: return@register true
            val state = scope.get<BingoState>()
            val teamService = scope.get<TeamService>()

            if (state.state != GameState.PLAYING) return@register true
            val team = teamService.getPlayerTeam(player) ?: return@register true

            sendTeamMessage(player, team, message.content)

            false
        }
    }
}