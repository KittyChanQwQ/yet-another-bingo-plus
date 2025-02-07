package me.jfenn.bingo.common.team

import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.commands.hasPermission
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.spawn.ChestService
import me.jfenn.bingo.common.spawn.PlayerController
import me.jfenn.bingo.common.spawn.SpawnService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.IExecutors
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.commands.ICommandManager
import me.jfenn.bingo.platform.commands.IExecutionContext
import me.jfenn.bingo.platform.commands.IExecutionSource
import net.minecraft.util.Formatting
import java.util.concurrent.CompletableFuture

class JoinCommand(
    commandManager: ICommandManager,
    private val text: TextProvider,
) : BingoComponent() {

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
                text.literal("⚠  ")
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
            teamService.joinTeam(player, team)

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
                    player.playerName,
                    team.getName(textProvider),
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
                        joinTeam(playerOrThrow, team)
                    }

                    player("player") { playerArg ->
                        requires { hasPermission(Permission.COMMAND_JOIN_PLAYER) && isNotProcessingTeams() }
                        executes {
                            val player = getArgument(playerArg)
                            joinTeam(player, team)
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val JOIN_COMMAND = "/join"
    }
}