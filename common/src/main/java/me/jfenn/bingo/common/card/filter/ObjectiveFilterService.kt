package me.jfenn.bingo.common.card.filter

import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.data.ScopedData

class ObjectiveFilterService(
    private val config: BingoConfig,
    private val data: ScopedData,
) {
    fun getPresetFilters(): Map<String, ObjectiveFilterList> {
        return config.itemFilterPresets + data.tierLists.keys.associateWith { tierList ->
            ObjectiveFilterList()
                .withFilter(ObjectiveFilter.Include(tierList))
                .withFilter(ObjectiveFilter.Exclude(ObjectiveFilter.UNCATEGORIZED))
                .withFilter(ObjectiveFilter.Exclude(ObjectiveFilter.UNOBTAINABLE))
        }
    }
}