package me.jfenn.bingo.common.card.tag

import me.jfenn.bingo.common.card.filter.ObjectiveFilter
import me.jfenn.bingo.common.card.objective.AdvancementObjectiveManager
import me.jfenn.bingo.common.card.objective.ItemObjectiveManager

class ObjectiveTagProvider(
    private val itemManager: ItemObjectiveManager,
    private val advancementManager: AdvancementObjectiveManager,
) : ITagProvider {
    override fun listTags(): Map<String, TagData> {
        return mapOf(
            ObjectiveFilter.ITEM to TagData(
                itemManager.list().toSet()
            ),
            ObjectiveFilter.ADVANCEMENT to TagData(
                advancementManager.list().toSet()
            )
        )
    }
}