package me.jfenn.bingo.common.autorestart

import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.state.PersistentStates
import me.jfenn.bingo.common.utils.measureTime
import me.jfenn.bingo.platform.IPersistentStateManager
import me.jfenn.bingo.platform.IServerWorldFactory
import me.jfenn.bingo.platform.config.IConfigManager
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.game.GameResetEvent
import org.slf4j.Logger
import kotlin.io.path.exists
import kotlin.random.Random

internal class ResetService(
    private val eventBus: IEventBus,
    private val state: BingoState,
    private val configManager: IConfigManager,
    private val config: BingoConfig,
    private val serverWorldFactory: IServerWorldFactory,
    private val persistentStateManager: IPersistentStateManager,
    private val persistentStates: PersistentStates,
    private val log: Logger,
) {

    fun resetGame() {
        if (state.isLobbyMode) {
            resetGameWorlds()
        } else {
            error("This shouldn't be happening. Tell fennifith to stop writing bad code.")
        }
    }

    fun copyWorldTemplate() {
        val templateDir = configManager.configDir.resolve(MOD_ID_BINGO).resolve(config.server.templateDir)
        when {
            config.server.templateEnabled && templateDir.exists() -> {
                log.info("[Reset] Copying world template from $templateDir...")
                serverWorldFactory.copyFromTemplate(templateDir)
            }
            config.server.fixedSeedEnabled -> {
                val seed = config.server.fixedSeed
                log.info("[Reset] Applying fixed world seed $seed...")
                serverWorldFactory.seed = seed
            }
            else -> {
                log.info("[Reset] Applying random world seed...")
                serverWorldFactory.seed = Random.nextLong()
            }
        }
    }

    private fun resetGameWorlds() = log.measureTime("Resetting worlds...") {
        // Clear scoreboards (otherwise this causes an error when recreated)
        log.info("[Reset] Clearing game data")
        eventBus.emit(GameResetEvent.BeforeClose)
        state.reset()

        log.info("[Reset] Recreating worlds")
        serverWorldFactory.recreateWorlds {
            eventBus.emit(GameResetEvent.Copy)
            copyWorldTemplate()
        }
        state.resetSeed(serverWorldFactory.seed)

        log.info("[Reset] Transferring game state")
        // This is important! Otherwise the bingo persistent state does not get copied into the new world data
        // (ScopeManager does this on initial setup, but it gets lost when the world is replaced)
        persistentStateManager.put(
            type = persistentStates.bingo,
            value = state,
        )

        log.info("[Reset] Teleporting players back to lobby")
        state.changeState(eventBus, GameState.PREGAME) // invokes createInitialCards in ScoredItemCheck

        eventBus.emit(GameResetEvent)
    }

}