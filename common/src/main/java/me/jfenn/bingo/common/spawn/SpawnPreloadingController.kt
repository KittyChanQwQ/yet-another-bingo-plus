package me.jfenn.bingo.common.spawn

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import net.minecraft.server.MinecraftServer
import net.minecraft.server.ServerTask
import net.minecraft.server.world.ServerWorld
import net.minecraft.world.chunk.ChunkStatus
import org.slf4j.Logger
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

/**
 * Wait until chunks around spawn are generated
 */
class SpawnPreloadingController(
    events: ScopedEvents,
    private val state: BingoState,
    private val spawnService: SpawnService,
    private val server: MinecraftServer,
    private val log: Logger,
) : BingoComponent() {

    private var startedLoading: Instant? = null
    private val chunksToLoad = Collections.newSetFromMap<Pair<Int, Int>>(ConcurrentHashMap())

    private fun waitForChunk(world: ServerWorld, pos: Pair<Int, Int>) {
        val (x, z) = pos
        world.getChunk(x, z, ChunkStatus.FULL)
        log.info("Loaded chunk ($x, $z) in ${startedLoading?.let { Clock.System.now() - it }}")
    }

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
            startedLoading = Clock.System.now()

            val world = spawnService.getSpawnDimension()
            val viewDistance = server.playerManager.viewDistance
            val chunkOffsets = buildList {
                for (x in -viewDistance..viewDistance) for (z in -viewDistance..viewDistance) {
                    if (isWithinDistance(x, z, viewDistance)) {
                        add(Pair(x, z))
                    }
                }
            }

            state.getRegisteredTeams()
                .mapNotNull { it.spawnpoint }
                .flatMap { (x, z) ->
                    chunkOffsets
                        .map { (offsetX, offsetZ) ->
                            Pair(x + offsetX, z + offsetZ)
                        }
                }
                .toSet()
                .also {
                    chunksToLoad.clear()
                    chunksToLoad.addAll(it)
                }

            log.info("Pre-generating ${chunksToLoad.size} chunks within a view distance of ${viewDistance}...")
            for (chunk in chunksToLoad.toList()) {
                server.send(ServerTask(server.ticks) {
                    waitForChunk(world, chunk)
                    chunksToLoad.remove(chunk)
                })
            }
        }

        events.onGameTick {
            if (state.state != GameState.PRELOADING) return@onGameTick

            if (chunksToLoad.isEmpty()) {
                log.info("Finished preloading spawn chunks in ${startedLoading?.let { Clock.System.now() - it }}")
                state.changeState(events, GameState.LOADING)
            }
        }
    }
}