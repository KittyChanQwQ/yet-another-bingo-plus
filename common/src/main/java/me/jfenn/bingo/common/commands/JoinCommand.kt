package me.jfenn.bingo.common.commands

import me.jfenn.bingo.api.IExecutors
import me.jfenn.bingo.api.IPlayerHandle
import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.api.commands.ICommandManager
import me.jfenn.bingo.api.commands.IExecutionContext
import me.jfenn.bingo.api.commands.IExecutionSource
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.spawn.ChestService
import me.jfenn.bingo.common.spawn.PlayerController
import me.jfenn.bingo.common.spawn.SpawnService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.concurrent.CompletableFuture

class JoinCommand(
    commandManager: ICommandManager,
) : BaseCommand() {

    private fun IExecutionSource.isNotProcessingTeams(): Boolean {
        val state = scope.get<BingoState>()
        return state.state != GameState.STARTING && state.state != GameState.PRELOADING
    }

    private fun IExecutionContext.joinTeam(
        player: IPlayerHandle,
        team: BingoTeam,
    ) {
        val state = scope.get<BingoState>()
        val teamService = scope.get<TeamService>()
        val spawnService = scope.get<SpawnService>()
        val playerController = scope.get<PlayerController>()
        val chestService = scope.get<ChestService>()
        val taskExecutor = scope.get<IExecutors.IServerTaskExecutor>()

        // If a player tries to /join during a game, and doesn't have COMMAND_JOIN_PLAYER
        if (state.state != GameState.PREGAME && !hasPermission(Permission.COMMAND_JOIN_PLAYER)) {
            sendMessage(
                Text.literal("⚠  ")
                    .append("Cannot join a team - The game has already started!")
                    .formatted(Formatting.YELLOW)
            )
            return
        }

        val createSpawnpointFuture = when (state.state) {
            GameState.PREGAME -> CompletableFuture.completedFuture(null)
            else -> spawnService.getTeamSpawnpointAsync(team)
        }

        createSpawnpointFuture.whenCompleteAsync({ _, _ ->
            teamService.joinTeam(player.player, team)

            if (state.state == GameState.PLAYING) {
                // if a player's team is changed in-game, move them to their new spawnpoint
                playerController.updateGameMode(player, forceReset = true)
            }

            if (team.chestSpawnpoint == null) {
                // if the team does not have a chest yet (i.e. was just created)
                chestService.createChestBlock(team)
            }
        }, taskExecutor)

        if (player != this.player) {
            val textProvider = scope.get<TextProvider>()
            sendFeedback(
                textProvider.string(
                    StringKey.CommandJoinSuccess,
                    player.name,
                    teamService.getTeamName(team),
                )
            )
        }
    }

    init {
        commandManager.register("join") {
            for (team in BingoTeam.TEAM_BLOCKS.values) {
                literal(team.id.removePrefix("bingo_")) {
                    requires { hasPermission(Permission.COMMAND_JOIN) && isNotProcessingTeams() }
                    executes {
                        joinTeam(scope.get<IPlayerManager>().forPlayer(player!!), team)
                    }

                    player("player") { playerArg ->
                        requires { hasPermission(Permission.COMMAND_JOIN_PLAYER) && isNotProcessingTeams() }
                        executes {
                            val player = getArgument(playerArg)
                            joinTeam(scope.get<IPlayerManager>().forPlayer(player), team)
                        }
                    }
                }
            }
        }
    }
}
