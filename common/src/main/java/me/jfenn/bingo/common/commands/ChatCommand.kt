package me.jfenn.bingo.common.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import me.jfenn.bingo.api.IScoreboardManager
import me.jfenn.bingo.common.scope.BingoKoin
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.koin.core.component.inject

class ChatCommand : BaseCommand() {

    private val scoreboardManager: IScoreboardManager by inject()

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
            .map { it.require(requireBingoInstance()) }
            .forEach { command ->
                command.then(
                    RequiredArgumentBuilder.argument<ServerCommandSource?, String?>(
                        "message",
                        StringArgumentType.greedyString()
                    )
                        .executesInScope { ctx ->
                            val player = ctx.source.player ?: return@executesInScope 0
                            val message = StringArgumentType.getString(ctx, "message")

                            val teamService = get<TeamService>()
                            val teamColor = teamService.getPlayerTeam(player)
                                ?.formatting
                                ?: Formatting.RESET

                            ctx.source.server?.playerManager?.broadcast(
                                Text.empty()
                                    .append(Text.literal("<"))
                                    .append(
                                        Text.literal(scoreboardManager.getPlayerName(player))
                                            .formatted(teamColor)
                                    )
                                    .append(Text.literal("> $message")),
                                false,
                            ) ?: return@executesInScope 0
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