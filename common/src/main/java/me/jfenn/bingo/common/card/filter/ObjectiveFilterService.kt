package me.jfenn.bingo.common.card.filter

import me.jfenn.bingo.common.card.tierlist.TierListState
import me.jfenn.bingo.common.config.BingoConfig

class ObjectiveFilterService(
    private val config: BingoConfig,
    private val tierLists: TierListState,
) {
    fun getPresetFilters(): Map<String, ObjectiveFilterList> {
        return config.itemFilterPresets + tierLists.tierLists.keys.associateWith { tierList ->
            ObjectiveFilterList()
                .withFilter(ObjectiveFilter.Include(tierList))
                .withFilter(ObjectiveFilter.Exclude(ObjectiveFilter.UNCATEGORIZED))
                .withFilter(ObjectiveFilter.Exclude(ObjectiveFilter.UNOBTAINABLE))
        }
    }
}