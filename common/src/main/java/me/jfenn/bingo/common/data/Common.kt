package me.jfenn.bingo.common.data

import me.jfenn.bingo.common.card.data.ObjectiveData
import me.jfenn.bingo.common.card.tag.TagData
import me.jfenn.bingo.common.card.tierlist.TierListConfig

typealias ObjectiveType = Map<String, ObjectiveData>
typealias TierListType = Map<String, TierListConfig>
typealias TagType = Map<String, TagData>
typealias UnobtainableItemsType = Set<String>
