package me.jfenn.bingo.integrations.chunky

import me.jfenn.bingo.platform.IServerWorldFactory
import org.popcraft.chunky.ChunkyProvider
import org.popcraft.chunky.iterator.PatternType
import org.popcraft.chunky.shape.ShapeType
import org.slf4j.Logger

class ChunkyImpl(
    private val config: ChunkyConfig,
    private val serverWorldFactory: IServerWorldFactory,
    private val log: Logger,
) : IChunkyApi {

    init {
        log.info("Integrations: Chunky is installed!")
    }

    override fun clearAllTasks() {
        try {
            val chunky = ChunkyProvider.get()
            serverWorldFactory.listWorlds().forEach {
                chunky.api.cancelTask(it.identifier)
                chunky.regionCache.clear(it.identifier)
            }
        } catch (e: Throwable) {
            log.error("Error clearing chunky state/tasks:", e)
        }
        try {
            val chunky = ChunkyProvider.get()
            val selectedWorld = chunky.selection.build().world().name
            chunky.selection.world(chunky.server.getWorld(selectedWorld).orElseThrow())
        } catch (e: Throwable) {
            log.error("Error clearing chunky selection reference:", e)
        }
    }

    override fun startPregen() {
        if (!config.enabled) return

        try {
            val chunky = ChunkyProvider.get()
            config.chunkyWorlds.forEach { (world, size) ->
                if (size > 0) {
                    chunky.api.startTask(world, ShapeType.SQUARE, 0.0, 0.0, size.toDouble(), size.toDouble(), PatternType.REGION)
                }
            }
        } catch (e: Throwable) {
            log.error("Error starting chunky pre-generation:", e)
        }
    }

    override fun cancelTasks() {
        try {
            val chunky = ChunkyProvider.get()
            serverWorldFactory.listWorlds().forEach {
                chunky.api.cancelTask(it.identifier)
            }
            chunky.scheduler.cancelTasks()
            chunky.trimTasks.let { task ->
                task.forEach { it.value.isCancelled = true }
                task.clear()
            }
        } catch (e: Throwable) {
            log.error("Error cancelling chunky pre-generation:", e)
        }
    }

}