package me.jfenn.bingo.common.autorestart

import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.api.IServerWorldFactory
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.menu.MenuController
import me.jfenn.bingo.common.scoreboard.ScoreboardService
import me.jfenn.bingo.common.spawn.SpawnService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.utils.measureTime
import net.minecraft.server.MinecraftServer
import org.slf4j.Logger
import kotlin.random.Random

class ResetService(
    private val events: ScopedEvents,
    private val config: BingoConfig,
    private val state: BingoState,
    private val playerManager: IPlayerManager,
    private val spawnService: SpawnService,
    private val scoreboardService: ScoreboardService,
    private val cardService: CardService,
    private val serverWorldFactory: IServerWorldFactory,
    private val menuController: MenuController,
    private val server: MinecraftServer,
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
        scoreboardService.clearScoreboards()
        state.reset()
        cardService.createInitialCards()

        log.info("[Reset] Recreating worlds")
        serverWorldFactory.recreateWorlds(server, Random.Default.nextLong()) {
            menuController.prepareLobbyFiles()
        }

        log.info("[Reset] Teleporting players back to lobby")
        state.changeState(events, GameState.PREGAME)

        // if any player is still in the world, teleport to the lobby
        for (player in playerManager.getPlayers()) {
            spawnService.teleportToLobby(player)
            state.playersJoinedIds += player.uuid
        }
    }

}