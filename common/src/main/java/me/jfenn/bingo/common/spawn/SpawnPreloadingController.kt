package me.jfenn.bingo.common.spawn

import kotlinx.datetime.Clock
import me.jfenn.bingo.api.IExecutors
import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.api.IServerWorldFactory
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text
import org.slf4j.Logger
import java.util.concurrent.CompletableFuture
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

/**
 * Wait until chunks around spawn are generated
 */
class SpawnPreloadingController(
    events: ScopedEvents,
    private val state: BingoState,
    private val config: BingoConfig,
    private val spawnService: SpawnService,
    private val serverWorldFactory: IServerWorldFactory,
    private val serverTaskExecutor: IExecutors.IServerTaskExecutor,
    private val playerManager: IPlayerManager,
    private val text: TextProvider,
    private val log: Logger,
) : BingoComponent() {

    private var chunkFutureList: List<CompletableFuture<*>> = emptyList()

    private fun isWithinDistance(dx: Int, dz: Int, distance: Int): Boolean {
        val deltaX = max(0, dx.absoluteValue - 1)
        val deltaZ = max(0, dz.absoluteValue - 1)
        val max = (max(deltaX, deltaZ) - 1).toLong().coerceAtLeast(0L)
        val min = min(deltaX, deltaZ).toLong()
        val distanceSquared = min * min + max * max
        return distanceSquared < (distance * distance).toLong()
    }

    init {
        events.onEnter(GameState.PRELOADING) {
            val startedLoading = Clock.System.now()

            val world = spawnService.getSpawnDimension()
            val viewDistance = config.server.preloadViewDistance
            val chunkOffsets = buildList {
                for (x in -viewDistance..viewDistance) for (z in -viewDistance..viewDistance) {
                    if (isWithinDistance(x, z, viewDistance)) {
                        add(Pair(x, z))
                    }
                }
            }

            val chunks = state.getRegisteredTeams()
                .mapNotNull { it.spawnpoint }
                .flatMap { (x, z) ->
                    chunkOffsets
                        .map { (offsetX, offsetZ) ->
                            Pair(x + offsetX, z + offsetZ)
                        }
                }
                .toSet()

            log.info("[SpawnPreloadingController] Pre-generating ${chunks.size} chunks within a view distance of ${viewDistance}...")
            val worldImpl = serverWorldFactory.forWorld(world)
            val futures = chunks.map { chunk ->
                worldImpl.getChunkAsync(chunk)
                    .whenComplete { _, _ ->
                        log.debug(
                            "[SpawnPreloadingController] Finished chunk {} in {}",
                            chunk,
                            Clock.System.now() - startedLoading
                        )
                    }
            }

            chunkFutureList = futures
            CompletableFuture.allOf(*futures.toTypedArray())
                .whenCompleteAsync({ _, _ ->
                    log.info("[SpawnPreloadingController] Finished preloading spawn chunks in ${Clock.System.now() - startedLoading}")
                    chunkFutureList = emptyList()
                    state.changeState(events, GameState.LOADING)
                }, serverTaskExecutor)
        }

        events.onGameTick {
            if (state.state != GameState.PRELOADING) return@onGameTick
            if (chunkFutureList.isEmpty()) return@onGameTick

            val doneCount = chunkFutureList.count { it.isDone }
            val percentage = doneCount.times(100f)
                .div(chunkFutureList.size)
                .let { String.format("%.0f", it) + "%" }

            val message = Text.empty()
                .append(text.string(StringKey.LobbyLoadingTerrain))
                .append(" $percentage ($doneCount/${chunkFutureList.size})")

            for (player in playerManager.getPlayers()) {
                player.sendHotbarMessage(message)
            }
        }
    }
}