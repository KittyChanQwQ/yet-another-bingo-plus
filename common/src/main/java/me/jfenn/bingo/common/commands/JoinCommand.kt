package me.jfenn.bingo.common.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import me.jfenn.bingo.api.IScoreboardManager
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.spawn.ChestService
import me.jfenn.bingo.common.spawn.PlayerController
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.sendWarningMessage
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.integrations.PermissionsIntegration
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import org.koin.core.scope.Scope

class JoinCommand : BaseCommand() {

    fun joinTeam(
        player: ServerPlayerEntity,
        team: BingoTeam,
        koinScope: Scope,
    ) {
        val state = koinScope.get<BingoState>()
        val teamService = koinScope.get<TeamService>()
        val playerController = koinScope.get<PlayerController>()
        val chestService = koinScope.get<ChestService>()

        teamService.joinTeam(player, team)

        if (state.state == GameState.PLAYING) {
            // if a player's team is changed in-game, move them to their new spawnpoint
            playerController.updateGameMode(player, forceReset = true)
        }

        if (team.chestSpawnpoint == null) {
            // if the team does not have a chest yet (i.e. was just created)
            chestService.createChestBlock(team)
        }
    }

    init {
        var joinCommand = LiteralArgumentBuilder.literal<ServerCommandSource>("join")

        for (team in BingoTeam.TEAM_BLOCKS.values) {
            joinCommand = joinCommand.then(
                LiteralArgumentBuilder.literal<ServerCommandSource>(team.id.removePrefix("bingo_"))
                    .then(
                        CommandManager.argument("player", EntityArgumentType.player())
                            .require(
                                requireBingoInstance(),
                                requirePermission(Permission.COMMAND_JOIN_PLAYER),
                            )
                            .executesInScope { ctx ->
                                val player = EntityArgumentType.getPlayer(ctx, "player")
                                joinTeam(player, team, this)

                                val textProvider = get<TextProvider>()
                                val scoreboardManager = get<IScoreboardManager>()
                                val teamService = get<TeamService>()
                                ctx.source.player?.sendMessage(
                                    textProvider.string(
                                        StringKey.CommandJoinSuccess,
                                        scoreboardManager.getPlayerName(player),
                                        teamService.getTeamName(team),
                                    )
                                )
                                1
                            }
                    )
                    .require(
                        requireBingoInstance(),
                        requirePermission(Permission.COMMAND_JOIN),
                    )
                    .executesInScope { ctx ->
                        val state = get<BingoState>()
                        val teamService = get<TeamService>()
                        val playerController = get<PlayerController>()
                        val permissions = get<PermissionsIntegration>()

                        val player = ctx.source.player ?: return@executesInScope 0

                        if (!permissions.hasPermission(player, Permission.COMMAND_JOIN_PLAYER)) {
                            if (state.state != GameState.PREGAME) {
                                player.sendWarningMessage("Cannot join a team - The game has already started!")
                                return@executesInScope 0
                            }
                        }

                        teamService.joinTeam(player, team)

                        if (state.state == GameState.PLAYING) {
                            // if a player's team is changed in-game, move them to their new spawnpoint
                            playerController.updateGameMode(player, forceReset = true)
                        }
                        1
                    }
            )
        }

        joinCommand.register()
    }
}
