package me.jfenn.bingo.common.data

import me.jfenn.bingo.common.card.tag.TagData
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.card.tierlist.TierListConfig
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.platform.event.model.ReloadEvent
import net.minecraft.server.MinecraftServer

class DataCommandService(
    private val scopedData: ScopedData,
    private val tagLoader: TagLoader,
    private val tierListLoader: TierListLoader,
    private val events: ScopedEvents,
    private val server: MinecraftServer,
) {
    private fun reload() {
        events.onDataReload(ReloadEvent(server, server.resourceManager))
    }

    fun addToTag(tagName: String, objective: String) {
        val tag = scopedData.tags[tagName] ?: TagData.EMPTY
        tagLoader.writeTag(tagName, tag.plus(objective))
        reload()
    }

    fun removeFromTag(tagName: String, objective: String) {
        val tag = scopedData.tags[tagName] ?: TagData.EMPTY
        tagLoader.writeTag(tagName, tag.minus(objective))
        reload()
    }

    fun addToTierList(listName: String, objective: String, tier: TierLabel?) {
        val tierList = scopedData.tierLists[listName] ?: TierListConfig.EMPTY
        tierListLoader.writeTierList(listName, tierList.plus(objective, tier))
        reload()
    }

    fun removeFromTierList(listName: String, objective: String) {
        val tierList = scopedData.tierLists[listName] ?: TierListConfig.EMPTY
        tierListLoader.writeTierList(listName, tierList.minus(objective))
        reload()
    }
}