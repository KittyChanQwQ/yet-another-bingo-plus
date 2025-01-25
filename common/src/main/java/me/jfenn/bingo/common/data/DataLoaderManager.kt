package me.jfenn.bingo.common.data

import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.controller.DatapackFunctionService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.utils.measureTime
import org.slf4j.Logger

internal class DataLoaderManager(
    events: ScopedEvents,
    private val log: Logger,
    private val scopedData: ScopedData,
    private val filterPresetsLoader: FilterPresetsLoader,
    private val objectiveLoader: ObjectiveLoader,
    private val tierListLoader: TierListLoader,
    private val tagLoader: TagLoader,
    private val unobtainableItemsLoader: UnobtainableItemsLoader,
    private val cardService: CardService,
    private val datapackFunctionService: DatapackFunctionService,
) {
    init {
        events.onDataReload { (_, manager) ->
            log.measureTime("[DataLoader] Reloading bingo data...") {
                scopedData.filterPresets = filterPresetsLoader.loadFilterPresets(manager)
                scopedData.objectives = objectiveLoader.loadObjectives(manager)
                scopedData.tierLists = tierListLoader.loadTierLists(manager)
                scopedData.tags = tagLoader.loadTags(manager)
                scopedData.unobtainableItems = emptySet()
                datapackFunctionService.loadCommands()
            }

            log.measureTime("[DataLoader] Determining unobtainable items...") {
                scopedData.unobtainableItems = unobtainableItemsLoader.loadUnobtainableItems(
                    scopedData.tierLists,
                    scopedData.tags,
                    manager
                )
            }

            cardService.validateLists()
        }
    }
}