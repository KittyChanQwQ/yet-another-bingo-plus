package me.jfenn.bingo

import com.mojang.brigadier.arguments.BoolArgumentType.bool
import com.mojang.brigadier.arguments.BoolArgumentType.getBool
import com.mojang.brigadier.arguments.IntegerArgumentType.getInteger
import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.card.GeneratorTier
import me.jfenn.bingo.card.TeamService
import me.jfenn.bingo.config.WinCondition
import me.jfenn.bingo.menu.MenuService
import me.jfenn.bingo.utils.allHeldStacks
import me.jfenn.bingo.utils.sendInfoMessage
import me.jfenn.bingo.utils.sendWarningMessage
import net.minecraft.item.Items
import net.minecraft.server.command.GameRuleCommand
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
    listOf("all", "g", "global")
        .map { literal<ServerCommandSource?>(it) }
        .forEach { command ->
            command.then(
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
        }

    literal<ServerCommandSource>("bingo")
        .requires { it.hasPermissionLevel(2) }
        .then(
            literal<ServerCommandSource>("itemdist")
                .then(run {
                    val args = GeneratorTier.values()
                        .map { tier -> argument<ServerCommandSource, Int>(tier.name, integer(0, 25)) }
                        .toMutableList()

                    var last = args.removeLast().executes { ctx ->
                        val itemDist = GeneratorTier.values()
                            .map { getInteger(ctx, it.name) }

                        if (itemDist.size == 5 && itemDist.sum() != 25) {
                            ctx.source.player?.sendWarningMessage("Item distribution must add up to 25!")
                            return@executes 0
                        }

                        BingoGame.config.itemDistribution = itemDist
                        MenuService.markDirty()
                        ctx.source.player?.sendInfoMessage("Item distribution changed: $itemDist")
                        1
                    }

                    while (args.isNotEmpty())
                        last = args.removeLast().then(last)

                    last
                })
        )
        .then(
            // Change the elytra mode feature setting (while the game is running)
            literal<ServerCommandSource>("elytra")
                .then(
                    argument<ServerCommandSource, Boolean>("true/false", bool())
                        .executes { ctx ->
                            val isElytra = getBool(ctx, "true/false")
                            BingoGame.config.isElytra = isElytra
                            MenuService.markDirty()

                            // if Elytra mode is turned off during a game, remove all players' elytra
                            if (BingoGame.state == GameState.PLAYING && !isElytra) {
                                ctx.source.server.playerManager.playerList.forEach { player ->
                                    player.allHeldStacks()
                                        .filter { it.item == Items.ELYTRA || it.item == Items.FIREWORK_ROCKET }
                                        .filter { it.nbt?.getBoolean(NBT_BINGO_VANISH) == true }
                                        .forEach {
                                            it.count = 0
                                        }
                                }
                            }

                            ctx.source.player?.sendInfoMessage("Elytra mode: $isElytra")
                            1
                        }
                )
        )
        .then(
            // Change the win condition setting (while the game is running)
            literal<ServerCommandSource>("wincon")
                .let {
                    val options = WinCondition.values()
                        .map {  wincon ->
                            literal<ServerCommandSource>(wincon.name)
                                .executes { ctx ->
                                    BingoGame.config.winCondition = wincon
                                    MenuService.markDirty()
                                    ctx.source.player?.sendInfoMessage("Win condition: ${wincon.name}")
                                    1
                                }
                        }
                        .toMutableList()

                    var last = it
                    while (options.isNotEmpty())
                        last = last.then(options.removeLast())

                    last
                }
        )
        .then(
            // Change the time limit setting (while the game is running)
            literal<ServerCommandSource>("timelimit")
                .then(
                    argument<ServerCommandSource, Int>("minutes", integer())
                        .executes { ctx ->
                            val minutes = getInteger(ctx, "minutes")
                            BingoGame.config.timerMinutes = minutes
                            MenuService.markDirty()
                            ctx.source.player?.sendInfoMessage("Time limit: ${minutes} minutes")
                            1
                        }
                )
                .then(
                    literal<ServerCommandSource?>("off")
                        .executes { ctx ->
                            BingoGame.config.timerMinutes = null
                            MenuService.markDirty()
                            ctx.source.player?.sendInfoMessage("Time limit: Off")
                            1
                        }
                )
        )
        .then(
            // Shuffle player teams by a number of teams
            literal<ServerCommandSource>("shuffleteams")
                .requires { BingoGame.state == GameState.PREGAME }
                .then(
                    argument<ServerCommandSource?, Int?>("teams", integer(1, BingoTeam.values().size))
                        .executes { ctx ->
                            val teams = getInteger(ctx, "teams")
                            TeamService.shuffleTeams(teams)

                            ctx.source.player?.sendInfoMessage("Teams have been shuffled!")
                            1
                        }
                )
        )
        .then(
            // re-roll the bingo card in a round (operator only)
            literal<ServerCommandSource>("reroll")
                .requires { BingoGame.state == GameState.PLAYING }
                .executes { ctx ->
                    BingoGame.rollCard()
                    ctx.source.player?.sendInfoMessage("Card re-rolled.")
                    1
                }
        )
        .then(
            // End the game, if running
            literal<ServerCommandSource>("end")
                .requires { it.hasPermissionLevel(2) && BingoGame.state == GameState.PLAYING }
                .executes { _ ->
                    BingoGame.end()
                    1
                }
        )
        .also { yield(it) }

    // Stop (restart) the server
    literal<ServerCommandSource>("restart")
        .requires { it.hasPermissionLevel(2) }
        .executes { ctx ->
            ctx.source.server.stop(false)
            1
        }
        .also { yield(it) }
}