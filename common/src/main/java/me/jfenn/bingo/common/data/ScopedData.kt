package me.jfenn.bingo.common.data

import me.jfenn.bingo.common.card.filter.ObjectiveFilterPresets
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.team.BingoTeamPreset

class ScopedData {
    var teamPresets: Map<BingoTeamKey, BingoTeamPreset> = emptyMap()
    var objectives: ObjectiveType = emptyMap()
    var tierLists: TierListType = emptyMap()
    var filterPresets: ObjectiveFilterPresets = ObjectiveFilterPresets.EMPTY
    var tags: TagType = emptyMap()
    var unobtainableItems: UnobtainableItemsType = emptySet()
}