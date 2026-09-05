package me.jfenn.bingo.common.spawn

import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.event.packet.ServerPacketEvents
import me.jfenn.bingo.common.ready.ReadyUpdatePacket
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.minus
import me.jfenn.bingo.common.utils.seconds
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.IExecutors
import me.jfenn.bingo.platform.IPlayerManager
import me.jfenn.bingo.platform.block.ChunkPosition
import me.jfenn.bingo.platform.event.IEventBus
import org.slf4j.Logger
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.CompletableFuture
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Wait until chunks around spawn are generated
 */
internal class SpawnPreloadingController(
    events: ScopedEvents,
    eventBus: IEventBus,
    private val state: BingoState,
    private val config: BingoConfig,
    private val spawnService: SpawnService,
    private val chestService: ChestService,
    private val serverTaskExecutor: IExecutors.IServerTaskExecutor,
    private val playerManager: IPlayerManager,
    private val packet: ServerPacketEvents,
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
        events.onEnter(GameState.STARTING) {
            if (state.isLobbyMode) {
                // spread player teams to spawnpoint
                spawnService.createSpawnpoints()
                    .thenRunAsync({
                        chestService.createChestSpawnpoints()

                        // change to the loading state (until all players have loaded terrain)
                        state.changeState(eventBus, GameState.PRELOADING)
                    }, serverTaskExecutor)
            } else {
                // change to the loading state (until all players have loaded terrain)
                state.changeState(eventBus, GameState.PRELOADING)
            }
        }

        events.onEnter(GameState.PRELOADING) {
            val startedLoading = Instant.now()

            val world = spawnService.getSpawnDimension()
            val viewDistance = config.server.preloadViewDistance
            val chunkOffsets = buildList {
                for (x in -viewDistance..viewDistance) for (z in -viewDistance..viewDistance) {
                    if (isWithinDistance(x, z, viewDistance)) {
                        add(ChunkPosition(x, z))
                    }
                }
            }

            val chunks = state.getRegisteredTeams()
                .mapNotNull { it.spawnpoint }
                .flatMap { (x, z) ->
                    chunkOffsets
                        .map { (offsetX, offsetZ) ->
                            ChunkPosition(x + offsetX, z + offsetZ)
                        }
                }
                .toSet()

            log.info("[SpawnPreloadingController] Pre-generating ${chunks.size} chunks within a view distance of ${viewDistance}...")
            val futures = chunks.map { chunk ->
                world.getChunkAsync(chunk)
                    .whenComplete { _, _ ->
                        log.debug(
                            "[SpawnPreloadingController] Finished chunk {} in {}",
                            chunk,
                            Instant.now() - startedLoading
                        )
                    }
            }

            chunkFutureList = futures
            CompletableFuture.allOf(*futures.toTypedArray())
                .whenCompleteAsync({ _, _ ->
                    log.info("[SpawnPreloadingController] Finished preloading spawn chunks in ${Instant.now() - startedLoading}")
                    chunkFutureList = emptyList()
                    state.changeState(eventBus, GameState.LOADING)
                }, serverTaskExecutor)
        }

        var spawnTimeoutAt = Instant.MIN
        events.onEnter(GameState.STARTING) {
            spawnTimeoutAt = Instant.now().plus(SPAWN_TIMEOUT_SECONDS, ChronoUnit.SECONDS)
        }

        events.onGameTick {
            if (state.state != GameState.STARTING) return@onGameTick
            val message = text.empty().append(text.string(StringKey.LobbyLoadingTerrain))

            val totalTime = Duration.ofSeconds(SPAWN_TIMEOUT_SECONDS).toMillis()
            val millisUntilTimeout = Instant.now()
                .until(spawnTimeoutAt, ChronoUnit.MILLIS)
                .coerceIn(0L, totalTime)
                .toFloat()
                .div(totalTime)
                .pow(3)
                .times(totalTime)
                .toLong()

            val updatePacket = ReadyUpdatePacket(
                isRunning = true,
                isReady = true,
                state = state.state,
                // remainingDuration only drives the progress bar, so this does not need to be accurate
                remainingDuration = Duration.ofMillis(millisUntilTimeout + totalTime),
                totalDuration = Duration.ofSeconds(SPAWN_TIMEOUT_SECONDS*2),
                readyPlayers = 0,
                totalPlayers = 0,
                title = text.string(StringKey.LobbyLoadingTerrain),
                subtitle = text.literal("0%"),
                canSendReady = false,
            )

            for (player in playerManager.getPlayers()) {
                when {
                    packet.readyUpdateV1.send(player, updatePacket) -> {}
                    else -> player.sendHotbarMessage(message)
                }
            }
        }

        events.onGameTick {
            if (state.state != GameState.PRELOADING) return@onGameTick
            if (chunkFutureList.isEmpty()) return@onGameTick

            val doneCount = chunkFutureList.count { it.isDone }
            val totalCount = chunkFutureList.size
            val percentage = doneCount.times(100f)
                .div(totalCount)
                .let { String.format("%.0f", it) + "%" }

            val message = text.empty()
                .append(text.string(StringKey.LobbyLoadingTerrain))
                .append(" $percentage ($doneCount/$totalCount)")

            val updatePacket = ReadyUpdatePacket(
                isRunning = true,
                isReady = true,
                state = state.state,
                // remainingDuration only drives the progress bar, so this does not need to be accurate
                remainingDuration = (totalCount - doneCount).seconds,
                totalDuration = (totalCount*2).seconds,
                readyPlayers = 0,
                totalPlayers = 0,
                title = text.string(StringKey.LobbyLoadingTerrain),
                subtitle = text.literal("$percentage ($doneCount/$totalCount)"),
                canSendReady = false,
            )

            for (player in playerManager.getPlayers()) {
                when {
                    packet.readyUpdateV1.send(player, updatePacket) -> {}
                    else -> player.sendHotbarMessage(message)
                }
            }
        }
    }
}