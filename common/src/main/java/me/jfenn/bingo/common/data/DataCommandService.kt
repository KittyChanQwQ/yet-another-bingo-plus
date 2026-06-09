package me.jfenn.bingo.common.data

import me.jfenn.bingo.common.card.tag.TagData
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.card.tierlist.TierListConfig
import me.jfenn.bingo.platform.IExecutors
import me.jfenn.bingo.platform.IMinecraftServer
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.model.ReloadEvent
import java.util.concurrent.CompletableFuture

internal class DataCommandService(
    private val scopedData: ScopedData,
    private val tagLoader: TagLoader,
    private val tierListLoader: TierListLoader,
    private val eventBus: IEventBus,
    private val server: IMinecraftServer,
    private val executors: IExecutors,
) {
    private fun reload() {
        val reloadEvent = ReloadEvent(server.resourceManager, executors.io, executors.main)
        eventBus.emit(ReloadEvent, reloadEvent)
            .let { CompletableFuture.allOf(*it.toTypedArray()) }
            .thenAcceptAsync({
                eventBus.emit(ReloadEvent.After, ReloadEvent.After())
            }, executors.main)
    }

    fun addToTag(tagName: String, objectives: Set<String>) {
        val tag = scopedData.tags[tagName] ?: TagData.EMPTY
        tagLoader.writeTag(tagName, tag.plus(objectives))
        reload()
    }

    fun removeFromTag(tagName: String, objectives: Set<String>) {
        val tag = scopedData.tags[tagName] ?: TagData.EMPTY
        tagLoader.writeTag(tagName, tag.minus(objectives))
        reload()
    }

    fun addToTierList(listName: String, objectives: List<Pair<String, TierLabel?>>) {
        val tierList = scopedData.tierLists[listName] ?: TierListConfig.EMPTY
        tierListLoader.writeTierList(listName, tierList.plus(objectives))
        reload()
    }

    fun removeFromTierList(listName: String, objectives: List<String>) {
        val tierList = scopedData.tierLists[listName] ?: TierListConfig.EMPTY
        tierListLoader.writeTierList(listName, tierList.minus(objectives))
        reload()
    }
}