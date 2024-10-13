package me.jfenn.bingo.common.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.game.GameEndReason
import me.jfenn.bingo.common.game.GameService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.utils.sendInfoMessage
import net.minecraft.server.command.ServerCommandSource
import kotlin.time.Duration.Companion.minutes

class BingoCommands : BaseCommand() {

    init {
        LiteralArgumentBuilder.literal<ServerCommandSource>("bingo")
            .require(requireBingoInstance())
            .then(
                // Change the time limit setting (while the game is running)
                LiteralArgumentBuilder.literal<ServerCommandSource>("timelimit")
                    .require(
                        requireState(GameState.PREGAME, GameState.PLAYING),
                        requirePermission(Permission.CONFIGURE_GAME),
                    )
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
                                events.onChangeOptions(Unit)
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
                                events.onChangeOptions(Unit)
                                ctx.source.player?.sendInfoMessage("Time limit: Off")
                                1
                            }
                    )
            )
            .then(
                // Shuffle player teams by a number of teams
                LiteralArgumentBuilder.literal<ServerCommandSource>("shuffleteams")
                    .require(
                        requireState(GameState.PREGAME),
                        requirePermission(Permission.CONFIGURE_GAME),
                    )
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
                // End the game, if running
                LiteralArgumentBuilder.literal<ServerCommandSource>("end")
                    .require(
                        requireState(GameState.PLAYING),
                        requirePermission(Permission.CONFIGURE_GAME),
                    )
                    .executesInScope { ctx ->
                        val gameService = get<GameService>()
                        val state = get<BingoState>()
                        state.isForfeit = true

                        val player = ctx.source.player?.let { get<IPlayerManager>().forPlayer(it) }
                        gameService.end(GameEndReason.Command(player?.name ?: "[unknown]"))
                        1
                    }
            )
            .register()
    }
}