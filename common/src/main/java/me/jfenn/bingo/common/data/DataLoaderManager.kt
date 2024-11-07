package me.jfenn.bingo.common.data

import me.jfenn.bingo.api.IExecutors
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.utils.measureTime
import net.minecraft.server.MinecraftServer
import org.slf4j.Logger

class DataLoaderManager(
    events: ScopedEvents,
    private val log: Logger,
    private val scopedData: ScopedData,
    private val objectiveLoader: ObjectiveLoader,
    private val tierListLoader: TierListLoader,
    private val tagLoader: TagLoader,
    private val unobtainableItemsLoader: UnobtainableItemsLoader,
    executors: IExecutors,
    server: MinecraftServer,
) {
    private val serverExecutor = executors.createServerTaskExecutor(server)

    init {
        events.onDataReload { (_, manager) ->
            log.measureTime("[DataLoader] Reloading bingo data...") {
                scopedData.objectives = objectiveLoader.loadObjectives(manager)
                scopedData.tierLists = tierListLoader.loadTierLists(manager)
                scopedData.tags = tagLoader.loadTags(manager)
                scopedData.unobtainableItems = emptySet()
            }

            serverExecutor.execute {
                log.measureTime("[DataLoader] Determining unobtainable items...") {
                    scopedData.unobtainableItems = unobtainableItemsLoader.loadUnobtainableItems(
                        scopedData.tierLists,
                        scopedData.tags,
                        manager
                    )
                }
            }
        }
    }
}