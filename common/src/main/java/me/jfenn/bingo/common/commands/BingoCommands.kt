package me.jfenn.bingo.common.commands

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import me.jfenn.bingo.common.GameService
import me.jfenn.bingo.common.NBT_BINGO_VANISH
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.config.*
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.utils.allHeldStacks
import me.jfenn.bingo.common.utils.formatTitle
import me.jfenn.bingo.common.utils.sendInfoMessage
import me.jfenn.bingo.common.utils.sendWarningMessage
import net.minecraft.item.Items
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.text.Texts
import net.minecraft.util.Formatting
import kotlin.time.Duration.Companion.minutes

class BingoCommands : BaseCommand() {
    init {
        LiteralArgumentBuilder.literal<ServerCommandSource>("bingo")
            .requireBingoInstance()
            .then(
                run {
                    var setCommand = LiteralArgumentBuilder.literal<ServerCommandSource>("set")
                        .requirePermission(permissions::canUseBingoSetCommand)

                    val fields = listOf(
                        PlayerSettings::bossbar,
                        PlayerSettings::scoreboard,
                        PlayerSettings::scoreboardAutoHide,
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
                                        .executesInScope { ctx ->
                                            val events = get<ScopedEvents>()
                                            val player = ctx.source.player ?: return@executesInScope 0
                                            val value = BoolArgumentType.getBool(ctx, "value")
                                            ConfigService.setPlayerConfig(events, player, field, value)
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
                    .requirePermission(permissions::canUseBingoCommand)
                    .then(run {
                        val args = TierList.entries
                            .map { tier ->
                                RequiredArgumentBuilder.argument<ServerCommandSource, Int>(
                                    tier.name,
                                    IntegerArgumentType.integer(0, 25)
                                )
                            }
                            .toMutableList()

                        var last = args.removeLast().executesInScope { ctx ->
                            val options = get<BingoOptions>()
                            val events = get<ScopedEvents>()
                            val cardService = get<CardService>()

                            val itemDist = TierList.entries
                                .map { IntegerArgumentType.getInteger(ctx, it.name) }

                            if (itemDist.size == 5 && itemDist.sum() > 25) {
                                ctx.source.player?.sendWarningMessage("Item distribution must add up to 25!")
                                return@executesInScope 0
                            }

                            options.itemDistribution = itemDist
                            events.onChangeConfig(Unit)
                            cardService.generateCard()
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
                    .requirePermission(permissions::canUseBingoCommand)
                    .then(
                        RequiredArgumentBuilder.argument<ServerCommandSource, Boolean>(
                            "true/false",
                            BoolArgumentType.bool()
                        )
                            .executesInScope { ctx ->
                                val state = get<BingoState>()
                                val options = get<BingoOptions>()
                                val events = get<ScopedEvents>()

                                val isElytra = BoolArgumentType.getBool(ctx, "true/false")
                                options.isElytra = isElytra
                                events.onChangeConfig(Unit)

                                // if Elytra mode is turned off during a game, remove all players' elytra
                                if (state.state == GameState.PLAYING && !isElytra) {
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
                    .requirePermission(permissions::canUseBingoCommand)
                    .let {
                        val options = WinCondition.entries
                            .map { wincon ->
                                LiteralArgumentBuilder.literal<ServerCommandSource>(wincon.name)
                                    .executesInScope { ctx ->
                                        val options = get<BingoOptions>()
                                        val events = get<ScopedEvents>()

                                        options.winCondition = wincon
                                        events.onChangeConfig(Unit)
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
                    .requirePermission(permissions::canUseBingoCommand)
                    .then(
                        RequiredArgumentBuilder.argument<ServerCommandSource, Int>(
                            "minutes",
                            IntegerArgumentType.integer(1)
                        )
                            .executesInScope { ctx ->
                                val options = get<BingoOptions>()
                                val events = get<ScopedEvents>()

                                val minutes = IntegerArgumentType.getInteger(ctx, "minutes")
                                options.timeLimit = minutes.minutes
                                events.onChangeConfig(Unit)
                                ctx.source.player?.sendInfoMessage("Time limit: $minutes minutes")
                                1
                            }
                    )
                    .then(
                        LiteralArgumentBuilder.literal<ServerCommandSource?>("off")
                            .executesInScope { ctx ->
                                val options = get<BingoOptions>()
                                val events = get<ScopedEvents>()

                                options.timeLimit = null
                                events.onChangeConfig(Unit)
                                ctx.source.player?.sendInfoMessage("Time limit: Off")
                                1
                            }
                    )
            )
            .then(
                // Shuffle player teams by a number of teams
                LiteralArgumentBuilder.literal<ServerCommandSource>("shuffleteams")
                    .requirePermission(permissions::canUseBingoCommand)
                    .requireState(GameState.PREGAME)
                    .then(
                        RequiredArgumentBuilder.argument<ServerCommandSource?, Int?>(
                            "teams",
                            IntegerArgumentType.integer(1, BingoTeam.TEAM_BLOCKS.size)
                        )
                            .executesInScope { ctx ->
                                val teamService = get<TeamService>()
                                val teams = IntegerArgumentType.getInteger(ctx, "teams")
                                teamService.shuffleTeams(teams)

                                ctx.source.player?.sendInfoMessage("Teams have been shuffled!")
                                1
                            }
                    )
            )
            .then(
                // show or set the current BINGO card seed
                LiteralArgumentBuilder.literal<ServerCommandSource>("seed")
                    .requirePermission(permissions::canUseBingoCommand)
                    .executesInScope { ctx ->
                        val state = get<BingoState>()

                        ctx.source.sendMessage(
                            Text.empty()
                                .append("ℹ  The current Bingo Card seed is: ")
                                .append(Texts.bracketedCopyable(state.seed.toString()))
                                .formatted(Formatting.AQUA)
                        )
                        1
                    }
                    .then(
                        RequiredArgumentBuilder.argument<ServerCommandSource, Long>(
                            "seed",
                            LongArgumentType.longArg(),
                        )
                            .requireState(GameState.PREGAME, GameState.PLAYING)
                            .executesInScope { ctx ->
                                val state = get<BingoState>()
                                val cardService = get<CardService>()

                                state.seed = LongArgumentType.getLong(ctx, "seed")
                                cardService.generateCard()

                                ctx.source.sendMessage(
                                    Text.empty()
                                        .append("ℹ  Changed the Bingo Card seed to: ")
                                        .append(Texts.bracketedCopyable(state.seed.toString()))
                                        .formatted(Formatting.AQUA)
                                )
                                1
                            }
                    )
            )
            .then(
                // re-roll the bingo card in a round (operator only)
                LiteralArgumentBuilder.literal<ServerCommandSource>("reroll")
                    .requireState(GameState.PREGAME, GameState.PLAYING)
                    .requirePermission(permissions::canUseBingoCommand)
                    .executesInScope { ctx ->
                        val cardService = get<CardService>()
                        cardService.shuffleCard()
                        ctx.source.player?.sendInfoMessage("Card re-rolled.")
                        1
                    }
            )
            .then(
                // Start the game, if in pre-game
                LiteralArgumentBuilder.literal<ServerCommandSource>("start")
                    .requireState(GameState.PREGAME)
                    .requirePermission(permissions::canUseBingoCommand)
                    .executesInScope { ctx ->
                        val gameService = get<GameService>()
                        // Start the game!
                        gameService.start(
                            sendFeedback = {
                                ctx.source.player?.sendWarningMessage(it)
                            },
                        )
                        1
                    }
            )
            .then(
                // End the game, if running
                LiteralArgumentBuilder.literal<ServerCommandSource>("end")
                    .requireState(GameState.PLAYING)
                    .requirePermission(permissions::canUseBingoCommand)
                    .executesInScope {
                        val gameService = get<GameService>()
                        gameService.end(null)
                        1
                    }
            )
            .register()
    }
}