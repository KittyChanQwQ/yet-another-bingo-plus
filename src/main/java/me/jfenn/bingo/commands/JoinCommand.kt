package me.jfenn.bingo.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.card.TeamService
import me.jfenn.bingo.state.GameState
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.KickCommand
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.dedicated.command.OpCommand

object JoinCommand {

    fun init() {
        registerCommands {
            var joinCommand = LiteralArgumentBuilder.literal<ServerCommandSource>("join")
                .requireState(GameState.PREGAME)

            for (team in BingoTeam.entries) {
                joinCommand = joinCommand.then(
                    LiteralArgumentBuilder.literal<ServerCommandSource>(team.name)
                        .executes {
                            val player = it.source.player ?: return@executes 0
                            TeamService.joinTeam(player, team)
                            1
                        }
                        .then(
                            CommandManager.argument("player", EntityArgumentType.player())
                                .requirePermission()
                                .executes { ctx ->
                                    val player = EntityArgumentType.getPlayer(ctx, "player")
                                    TeamService.joinTeam(player, team)
                                    1
                                }
                        )
                )
            }

            yield(joinCommand)
        }
    }

}