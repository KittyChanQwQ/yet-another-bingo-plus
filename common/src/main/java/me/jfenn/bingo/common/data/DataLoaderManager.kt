package me.jfenn.bingo.common.data

import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.controller.DatapackFunctionService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.text.MessageService
import me.jfenn.bingo.common.utils.measureTime
import me.jfenn.bingo.platform.IExecutors
import org.slf4j.Logger
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

internal class DataLoaderManager(
    events: ScopedEvents,
    private val log: Logger,
    private val scopedData: ScopedData,
    private val filterPresetsLoader: FilterPresetsLoader,
    private val objectiveLoader: ObjectiveLoader,
    private val tierListLoader: TierListLoader,
    private val tagLoader: TagLoader,
    private val teamPresetLoader: TeamPresetLoader,
    private val unobtainableItemsLoader: UnobtainableItemsLoader,
    private val cardService: CardService,
    private val datapackFunctionService: DatapackFunctionService,
    private val messageService: MessageService,
    private val serverTaskExecutor: IExecutors.IServerTaskExecutor,
) {
    init {
        events.onDataReload { (_, manager) ->
            log.measureTime("[DataLoader] Reloading bingo data...") {
                scopedData.teamPresets = teamPresetLoader.loadTeamPresets(manager)
                scopedData.filterPresets = filterPresetsLoader.loadFilterPresets(manager)
                scopedData.objectives = objectiveLoader.loadObjectives(manager)
                scopedData.tierLists = tierListLoader.loadTierLists(manager)
                scopedData.tags = tagLoader.loadTags(manager)
                scopedData.unobtainableItems = emptySet()
                datapackFunctionService.loadCommands()
                messageService.loadMessages(manager)
            }

            log.measureTime("[DataLoader] Determining unobtainable items...") {
                scopedData.unobtainableItems = unobtainableItemsLoader.loadUnobtainableItems(
                    scopedData.tierLists,
                    scopedData.tags,
                    manager
                )
            }

            // Validate after the next tick(s), once any datapacks/scoreboards are loaded
            CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS, serverTaskExecutor)
                .execute { cardService.validateLists() }
        }
    }
}