package me.jfenn.bingo.common.autorestart

import me.jfenn.bingo.api.IExecutors
import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.api.IServerWorldFactory
import me.jfenn.bingo.api.commands.ICommandManager
import me.jfenn.bingo.api.commands.IExecutionContext
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.commands.hasPermission
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.menu.MenuController
import me.jfenn.bingo.common.scoreboard.ScoreboardService
import me.jfenn.bingo.common.spawn.SpawnService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.utils.measureTime
import net.minecraft.server.MinecraftServer
import org.slf4j.Logger

class BingoResetCommand(
    private val log: Logger,
    commandManager: ICommandManager
) {

    fun IExecutionContext.reset() {
        val events = scope.get<ScopedEvents>()
        val state = scope.get<BingoState>()
        val playerManager = scope.get<IPlayerManager>()
        val spawnService = scope.get<SpawnService>()
        val serverWorldFactory = scope.get<IServerWorldFactory>()
        val server = scope.get<MinecraftServer>()
        val executors: IExecutors = scope.get()
        if (!state.isLobbyMode) return

        // Clear scoreboards (otherwise this causes an error when recreated)
        log.info("[Reset] Clearing game data")
        scope.get<ScoreboardService>().clearScoreboards()
        state.reset()
        scope.get<CardService>().createInitialCards()

        executors.createServerTaskExecutor(server).execute {
            // delete each world
            for (world in serverWorldFactory.listWorlds(server)) {
                log.info("[Reset] Closing world ${world.identifier}")
                world.close()

                log.info("[Reset] Deleting ${world.directory}")
                world.directory.resolve("region").toFile().takeIf { it.exists() }?.deleteRecursively()
                world.directory.resolve("poi").toFile().takeIf { it.exists() }?.deleteRecursively()
                world.directory.resolve("entities").toFile().takeIf { it.exists() }?.deleteRecursively()
            }

            log.info("[Reset] Recreating worlds")
            scope.get<MenuController>().prepareLobbyFiles()
            serverWorldFactory.recreateWorlds(server)

            log.info("[Reset] Teleporting players back to lobby")
            state.changeState(events, GameState.PREGAME)

            // if any player is still in the world, teleport to the lobby
            for (player in playerManager.getPlayers()) {
                spawnService.teleportToLobby(player)
                state.playersJoinedIds += player.uuid
            }
        }
    }

    init {
        commandManager.register("bingo") {
            literal("reset") {
                requires { hasPermission(Permission.COMMAND_RESTART) }
                executes {
                    log.measureTime("Resetting worlds...") {
                        reset()
                    }
                }
            }
        }
    }
}