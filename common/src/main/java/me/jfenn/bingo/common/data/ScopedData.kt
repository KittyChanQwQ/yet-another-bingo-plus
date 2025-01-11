package me.jfenn.bingo.common.data

import me.jfenn.bingo.common.card.filter.ObjectiveFilterPresets

class ScopedData {
    var objectives: ObjectiveType = emptyMap()
    var tierLists: TierListType = emptyMap()
    var filterPresets: ObjectiveFilterPresets = ObjectiveFilterPresets.EMPTY
    var tags: TagType = emptyMap()
    var unobtainableItems: UnobtainableItemsType = emptySet()
}