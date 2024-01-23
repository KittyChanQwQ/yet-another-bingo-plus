package me.jfenn.bingo.common.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.controller.PlayerController
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.utils.sendWarningMessage
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource

class JoinCommand : BaseCommand() {

    init {
        var joinCommand = LiteralArgumentBuilder.literal<ServerCommandSource>("join")
            .requireBingoInstance()

        for (team in BingoTeam.TEAM_BLOCKS.values) {
            joinCommand = joinCommand.then(
                LiteralArgumentBuilder.literal<ServerCommandSource>(team.name.lowercase())
                    .then(
                        CommandManager.argument("player", EntityArgumentType.player())
                            .requirePermission(permissions::canUseJoinPlayerCommand)
                            .executesInScope { ctx ->
                                val state = get<BingoState>()
                                val teamService = get<TeamService>()
                                val playerController = get<PlayerController>()

                                val player = EntityArgumentType.getPlayer(ctx, "player")
                                teamService.joinTeam(player, team)

                                if (state.state == GameState.PLAYING) {
                                    // if a player's team is changed in-game, move them to their new spawnpoint
                                    playerController.updateGameMode(player, forceReset = true)
                                }
                                1
                            }
                    )
                    .requirePermission(permissions::canUseJoinCommand)
                    .executesInScope { ctx ->
                        val state = get<BingoState>()
                        val teamService = get<TeamService>()

                        val player = ctx.source.player ?: return@executesInScope 0

                        if (state.state != GameState.PREGAME) {
                            player.sendWarningMessage("Cannot join a team - The game has already started!")
                            return@executesInScope 0
                        }

                        teamService.joinTeam(player, team)
                        1
                    }
            )
        }

        joinCommand.register()
    }
}
