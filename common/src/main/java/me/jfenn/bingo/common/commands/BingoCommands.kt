package me.jfenn.bingo.common.commands

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import me.jfenn.bingo.common.Bingo
import me.jfenn.bingo.common.BingoGame
import me.jfenn.bingo.common.NBT_BINGO_VANISH
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.config.PlayerSettings
import me.jfenn.bingo.common.config.TierList
import me.jfenn.bingo.common.config.WinCondition
import me.jfenn.bingo.common.integrations.BingoCardController
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.utils.allHeldStacks
import me.jfenn.bingo.common.utils.formatTitle
import me.jfenn.bingo.common.utils.sendInfoMessage
import me.jfenn.bingo.common.utils.sendWarningMessage
import net.minecraft.item.Items
import net.minecraft.server.command.ServerCommandSource
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

object BingoCommands {
    fun init() = registerCommands {
        LiteralArgumentBuilder.literal<ServerCommandSource>("bingo")
            .then(
                run {
                    var setCommand = LiteralArgumentBuilder.literal<ServerCommandSource>("set")
                    val fields = listOf(
                        PlayerSettings::scoreboard,
                        PlayerSettings::bossbar,
                        PlayerSettings::leadingMessages,
                        PlayerSettings::scoreMessages,
                    )

                    for (field in fields) {
                        setCommand = setCommand.then(
                            LiteralArgumentBuilder.literal<ServerCommandSource>(field.name)
                                .then(
                                    RequiredArgumentBuilder.argument<ServerCommandSource, Boolean>(
                                        "value",
                                        BoolArgumentType.bool()
                                    )
                                        .executes { ctx ->
                                            val player = ctx.source.player ?: return@executes 0
                                            val value = BoolArgumentType.getBool(ctx, "value")
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
                LiteralArgumentBuilder.literal<ServerCommandSource>("itemdist")
                    .requirePermission(2)
                    .then(run {
                        val args = TierList.entries
                            .map { tier ->
                                RequiredArgumentBuilder.argument<ServerCommandSource, Int>(
                                    tier.name,
                                    IntegerArgumentType.integer(0, 25)
                                )
                            }
                            .toMutableList()

                        var last = args.removeLast().executes { ctx ->
                            val context = Bingo.getContext(ctx.source.server) ?: return@executes 0
                            val itemDist = TierList.entries
                                .map { IntegerArgumentType.getInteger(ctx, it.name) }

                            if (itemDist.size == 5 && itemDist.sum() != 25) {
                                ctx.source.player?.sendWarningMessage("Item distribution must add up to 25!")
                                return@executes 0
                            }

                            context.config.itemDistribution = itemDist
                            context.menu?.markDirty()
                            BingoCardController.rollCard(context)
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
                LiteralArgumentBuilder.literal<ServerCommandSource>("elytra")
                    .requirePermission(2)
                    .then(
                        RequiredArgumentBuilder.argument<ServerCommandSource, Boolean>(
                            "true/false",
                            BoolArgumentType.bool()
                        )
                            .executes { ctx ->
                                val context = Bingo.getContext(ctx.source.server) ?: return@executes 0
                                val isElytra = BoolArgumentType.getBool(ctx, "true/false")
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
                LiteralArgumentBuilder.literal<ServerCommandSource>("wincon")
                    .requirePermission(2)
                    .let {
                        val options = WinCondition.entries
                            .map {  wincon ->
                                LiteralArgumentBuilder.literal<ServerCommandSource>(wincon.name)
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
                LiteralArgumentBuilder.literal<ServerCommandSource>("timelimit")
                    .requirePermission(2)
                    .then(
                        RequiredArgumentBuilder.argument<ServerCommandSource, Int>(
                            "minutes",
                            IntegerArgumentType.integer()
                        )
                            .executes { ctx ->
                                val context = Bingo.getContext(ctx.source.server) ?: return@executes 0
                                val minutes = IntegerArgumentType.getInteger(ctx, "minutes")
                                context.config.timeLimit = minutes.minutes
                                context.menu?.markDirty()
                                ctx.source.player?.sendInfoMessage("Time limit: $minutes minutes")
                                1
                            }
                    )
                    .then(
                        LiteralArgumentBuilder.literal<ServerCommandSource?>("off")
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
                LiteralArgumentBuilder.literal<ServerCommandSource>("shuffleteams")
                    .requirePermission(2)
                    .requireState(GameState.PREGAME)
                    .then(
                        RequiredArgumentBuilder.argument<ServerCommandSource?, Int?>(
                            "teams",
                            IntegerArgumentType.integer(1, BingoTeam.entries.size)
                        )
                            .executes { ctx ->
                                val context = Bingo.getContext(ctx.source.server) ?: return@executes 0
                                val teams = IntegerArgumentType.getInteger(ctx, "teams")
                                TeamService.shuffleTeams(context, teams)

                                ctx.source.player?.sendInfoMessage("Teams have been shuffled!")
                                1
                            }
                    )
            )
            .then(
                // re-roll the bingo card in a round (operator only)
                LiteralArgumentBuilder.literal<ServerCommandSource>("reroll")
                    .requirePermission(2)
                    .requireState(GameState.PREGAME, GameState.PLAYING)
                    .executes { ctx ->
                        val context = Bingo.getContext(ctx.source.server) ?: return@executes 0
                        // Change the BINGO seed to a new value
                        context.state.seed = Random.nextLong()
                        // Re-roll the card
                        BingoCardController.rollCard(context)
                        ctx.source.player?.sendInfoMessage("Card re-rolled.")
                        1
                    }
            )
            .then(
                // End the game, if running
                LiteralArgumentBuilder.literal<ServerCommandSource>("end")
                    .requirePermission(2)
                    .requireState(GameState.PLAYING)
                    .executes { ctx ->
                        val context = Bingo.getContext(ctx.source.server) ?: return@executes 0
                        BingoGame.end(context, null)
                        1
                    }
            )
            .also { yield(it) }
    }
}