package me.jfenn.bingo

import com.mojang.brigadier.arguments.BoolArgumentType.bool
import com.mojang.brigadier.arguments.BoolArgumentType.getBool
import com.mojang.brigadier.arguments.IntegerArgumentType.getInteger
import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.card.TeamService
import me.jfenn.bingo.commands.Coords
import me.jfenn.bingo.commands.requireState
import me.jfenn.bingo.config.ConfigService
import me.jfenn.bingo.config.PlayerSettings
import me.jfenn.bingo.config.TierList
import me.jfenn.bingo.config.WinCondition
import me.jfenn.bingo.state.GameState
import me.jfenn.bingo.utils.allHeldStacks
import me.jfenn.bingo.utils.formatTitle
import me.jfenn.bingo.utils.sendInfoMessage
import me.jfenn.bingo.utils.sendWarningMessage
import net.minecraft.item.Items
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import kotlin.time.Duration.Companion.minutes

/**
 * Return a sequence of commands related to the game
 */
fun commands() : Sequence<LiteralArgumentBuilder<ServerCommandSource>> = sequence {
    yieldAll(Coords.register())

    // send a message to all teams, outside of the team-local chat
    listOf("all", "g", "global")
        .map { literal<ServerCommandSource?>(it) }
        .forEach { command ->
            command.then(
                argument<ServerCommandSource?, String?>("message", greedyString())
                    .executes { ctx ->
                        val player = ctx.source.player ?: return@executes 0
                        val message = getString(ctx, "message")

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

    literal<ServerCommandSource>("bingo")
        .then(
            run {
                var setCommand = literal<ServerCommandSource>("set")
                val fields = listOf(
                    PlayerSettings::scoreboard,
                    PlayerSettings::bossbar,
                    PlayerSettings::leadingMessages,
                    PlayerSettings::scoreMessages,
                )

                for (field in fields) {
                    setCommand = setCommand.then(
                        literal<ServerCommandSource>(field.name)
                            .then(
                                argument<ServerCommandSource, Boolean>("value", bool())
                                    .executes { ctx ->
                                        val player = ctx.source.player ?: return@executes 0
                                        val value = getBool(ctx, "value")
                                        ConfigService.setPlayerConfig(player, field, value)
                                        player.sendInfoMessage("${field.name.formatTitle()}: $value")
                                        1
                                    }
                            )
                    )
                }

                setCommand
            }
        )
        .then(
            literal<ServerCommandSource>("itemdist")
                .requires { it.hasPermissionLevel(2) }
                .then(run {
                    val args = TierList.entries
                        .map { tier -> argument<ServerCommandSource, Int>(tier.name, integer(0, 25)) }
                        .toMutableList()

                    var last = args.removeLast().executes { ctx ->
                        val context = Bingo.getContext(ctx.source.server) ?: return@executes 0
                        val itemDist = TierList.entries
                            .map { getInteger(ctx, it.name) }

                        if (itemDist.size == 5 && itemDist.sum() != 25) {
                            ctx.source.player?.sendWarningMessage("Item distribution must add up to 25!")
                            return@executes 0
                        }

                        context.config.itemDistribution = itemDist
                        context.menu?.markDirty()
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
                .requires { it.hasPermissionLevel(2) }
                .then(
                    argument<ServerCommandSource, Boolean>("true/false", bool())
                        .executes { ctx ->
                            val context = Bingo.getContext(ctx.source.server) ?: return@executes 0
                            val isElytra = getBool(ctx, "true/false")
                            context.config.isElytra = isElytra
                            context.menu?.markDirty()

                            // if Elytra mode is turned off during a game, remove all players' elytra
                            if (context.state.state == GameState.PLAYING && !isElytra) {
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
                .requires { it.hasPermissionLevel(2) }
                .let {
                    val options = WinCondition.entries
                        .map {  wincon ->
                            literal<ServerCommandSource>(wincon.name)
                                .executes { ctx ->
                                    val context = Bingo.getContext(ctx.source.server) ?: return@executes 0
                                    context.config.winCondition = wincon
                                    context.menu?.markDirty()
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
                .requires { it.hasPermissionLevel(2) }
                .then(
                    argument<ServerCommandSource, Int>("minutes", integer())
                        .executes { ctx ->
                            val context = Bingo.getContext(ctx.source.server) ?: return@executes 0
                            val minutes = getInteger(ctx, "minutes")
                            context.config.timeLimit = minutes.minutes
                            context.menu?.markDirty()
                            ctx.source.player?.sendInfoMessage("Time limit: $minutes minutes")
                            1
                        }
                )
                .then(
                    literal<ServerCommandSource?>("off")
                        .executes { ctx ->
                            val context = Bingo.getContext(ctx.source.server) ?: return@executes 0
                            context.config.timeLimit = null
                            context.menu?.markDirty()
                            ctx.source.player?.sendInfoMessage("Time limit: Off")
                            1
                        }
                )
        )
        .then(
            // Shuffle player teams by a number of teams
            literal<ServerCommandSource>("shuffleteams")
                .requires { it.hasPermissionLevel(2) }
                .requireState(GameState.PREGAME)
                .then(
                    argument<ServerCommandSource?, Int?>("teams", integer(1, BingoTeam.entries.size))
                        .executes { ctx ->
                            val context = Bingo.getContext(ctx.source.server) ?: return@executes 0
                            val teams = getInteger(ctx, "teams")
                            TeamService.shuffleTeams(context, teams)

                            ctx.source.player?.sendInfoMessage("Teams have been shuffled!")
                            1
                        }
                )
        )
        .then(
            // re-roll the bingo card in a round (operator only)
            literal<ServerCommandSource>("reroll")
                .requires { it.hasPermissionLevel(2) }
                .requireState(GameState.PLAYING)
                .executes { ctx ->
                    val context = Bingo.getContext(ctx.source.server) ?: return@executes 0
                    BingoGame.rollCard(context)
                    ctx.source.player?.sendInfoMessage("Card re-rolled.")
                    1
                }
        )
        .then(
            // End the game, if running
            literal<ServerCommandSource>("end")
                .requires { it.hasPermissionLevel(2) }
                .requireState(GameState.PLAYING)
                .executes { ctx ->
                    val context = Bingo.getContext(ctx.source.server) ?: return@executes 0
                    BingoGame.end(context, null)
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