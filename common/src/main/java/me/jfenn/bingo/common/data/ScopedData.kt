package me.jfenn.bingo.common.data

class ScopedData {
    var objectives: ObjectiveType = emptyMap()
    var tierLists: TierListType = emptyMap()
    var tags: TagType = emptyMap()
    var unobtainableItems: UnobtainableItemsType = emptySet()
}