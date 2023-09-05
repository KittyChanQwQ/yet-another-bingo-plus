package me.jfenn.bingo

import com.mojang.brigadier.arguments.IntegerArgumentType.getInteger
import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.card.TeamService
import me.jfenn.bingo.utils.sendInfoMessage
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

/**
 * Return a sequence of commands related to the game
 */
fun commands() : Sequence<LiteralArgumentBuilder<ServerCommandSource>> = sequence {
    // send current player coordinates to other team members
    literal<ServerCommandSource?>("coords")
        .requires { ctx -> ctx.player?.let { BingoTeam.fromPlayer(it) } != null }
        .then(
            argument<ServerCommandSource?, String?>("message", greedyString())
                .executes { ctx ->
                    val player = ctx.source.player ?: return@executes 0
                    val team = BingoTeam.fromPlayer(player) ?: return@executes 0
                    val message = getString(ctx, "message")

                    team.broadcastMessage(
                        Text.empty()
                            .append(Text.literal("TEAM ${player.entityName}").formatted(team.formatting))
                            .append(Text.literal(with(player.blockPos) { ": [$x, $y, $z] $message" }))
                    )
                    1
                }
        )
        .executes { ctx ->
            val player = ctx.source.player ?: return@executes 0
            val team = BingoTeam.fromPlayer(player) ?: return@executes 0

            team.broadcastMessage(
                Text.empty()
                    .append(Text.literal("TEAM ${player.entityName}").formatted(team.formatting))
                    .append(Text.literal(with(player.blockPos) { ": [$x, $y, $z]" }))
            )
            1
        }
        .also { yield(it) }

    // send a message to all teams, outside of the team-local chat
    literal<ServerCommandSource?>("all")
        .then(
            argument<ServerCommandSource?, String?>("message", greedyString())
                .executes { ctx ->
                    val player = ctx.source.player ?: return@executes 0
                    val message = getString(ctx, "message")

                    minecraftServer.playerManager.broadcast(
                        Text.empty()
                            .append(Text.literal("<"))
                            .append(player.name)
                            .append(Text.literal("> $message")),
                        false,
                    )
                    1
                }
        )
        .also { yield(it) }

    // re-roll the bingo card in a round (operator only)
    literal<ServerCommandSource?>("reroll")
        .requires { it.hasPermissionLevel(2) && BingoGame.state == GameState.PLAYING }
        .executes { ctx ->
            BingoGame.rollCard()
            ctx.source.player?.sendInfoMessage("Card re-rolled.")
            1
        }
        .also { yield(it) }

    literal<ServerCommandSource?>("shuffleteams")
        .requires { it.hasPermissionLevel(2) && BingoGame.state == GameState.PREGAME }
        .then(
            argument<ServerCommandSource?, Int?>("teams", integer(1, BingoTeam.values().size))
                .executes { ctx ->
                    val teams = getInteger(ctx, "teams")
                    TeamService.shuffleTeams(teams)

                    ctx.source.player?.sendInfoMessage("Teams have been shuffled!")
                    1
                }
        )
        .also { yield(it) }

    literal<ServerCommandSource>("restart")
        .requires { it.hasPermissionLevel(2) }
        .executes { ctx ->
            ctx.source.server.stop(false)
            1
        }
        .also { yield(it) }
}