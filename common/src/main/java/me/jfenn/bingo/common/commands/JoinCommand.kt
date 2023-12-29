package me.jfenn.bingo.common.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import me.jfenn.bingo.common.Bingo
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.integrations.PlayerController
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.utils.sendWarningMessage
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource

object JoinCommand {

    fun init() {
        registerCommands {
            var joinCommand = LiteralArgumentBuilder.literal<ServerCommandSource>("join")
                .requireBingoInstance()

            for (team in BingoTeam.entries) {
                joinCommand = joinCommand.then(
                    LiteralArgumentBuilder.literal<ServerCommandSource>(team.name)
                        .then(
                            CommandManager.argument("player", EntityArgumentType.player())
                                .requirePermission()
                                .executes { ctx ->
                                    val context = Bingo.getContext(ctx.source.server) ?: return@executes 0
                                    val player = EntityArgumentType.getPlayer(ctx, "player")
                                    TeamService.joinTeam(player, team)

                                    if (context.state.state == GameState.PLAYING) {
                                        // if a player's team is changed in-game, move them to their new spawnpoint
                                        PlayerController.updateGameMode(context, player, forceReset = true)
                                    }
                                    1
                                }
                        )
                        .executes { ctx ->
                            val context = Bingo.getContext(ctx.source.server) ?: return@executes 0
                            val player = ctx.source.player ?: return@executes 0

                            if (context.state.state != GameState.PREGAME) {
                                player.sendWarningMessage("Cannot join a team - The game has already started!")
                                return@executes 0
                            }

                            TeamService.joinTeam(player, team)
                            1
                        }
                )
            }

            yield(joinCommand)
        }
    }

}