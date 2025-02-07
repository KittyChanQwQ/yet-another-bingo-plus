package me.jfenn.bingo.common.autorestart

import me.jfenn.bingo.common.bossbar.BossBarService
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.menu.MenuController
import me.jfenn.bingo.common.scoreboard.ScoreboardService
import me.jfenn.bingo.common.spawn.SpawnService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.state.PersistentStates
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.utils.measureTime
import me.jfenn.bingo.platform.IPersistentStateManager
import me.jfenn.bingo.platform.IPlayerManager
import me.jfenn.bingo.platform.IServerWorldFactory
import net.minecraft.server.MinecraftServer
import org.slf4j.Logger
import kotlin.random.Random

internal class ResetService(
    private val events: ScopedEvents,
    private val config: BingoConfig,
    private val state: BingoState,
    private val playerManager: IPlayerManager,
    private val spawnService: SpawnService,
    private val teamService: TeamService,
    private val scoreboardService: ScoreboardService,
    private val bossBarService: BossBarService,
    private val serverWorldFactory: IServerWorldFactory,
    private val menuController: MenuController,
    private val server: MinecraftServer,
    private val persistentStateManager: IPersistentStateManager,
    private val persistentStates: PersistentStates,
    private val log: Logger,
) {

    fun resetGame() {
        if (config.nextRoundCompatibilityMode) {
            server.stop(false)
        } else {
            resetGameWorlds()
        }
    }

    private fun resetGameWorlds() = log.measureTime("Resetting worlds...") {
        // Clear scoreboards (otherwise this causes an error when recreated)
        log.info("[Reset] Clearing game data")
        teamService.clearTeams()
        scoreboardService.clearScoreboards()
        bossBarService.clearBossBars()
        state.reset()

        log.info("[Reset] Recreating worlds")
        serverWorldFactory.recreateWorlds(server, Random.Default.nextLong()) {
            menuController.prepareLobbyFiles()
        }

        log.info("[Reset] Transferring game state")
        // This is important! Otherwise the bingo persistent state does not get copied into the new world data
        // (ScopeManager does this on initial setup, but it gets lost when the world is replaced)
        persistentStateManager.put(
            world = server.overworld,
            type = persistentStates.bingo,
            value = state,
        )

        log.info("[Reset] Teleporting players back to lobby")
        state.changeState(events, GameState.PREGAME) // invokes createInitialCards in ScoredItemCheck

        // if any player is still in the world, teleport to the lobby
        for (player in playerManager.getPlayers()) {
            spawnService.teleportToLobby(player)
            state.playersJoinedIds += player.uuid
        }
    }

}