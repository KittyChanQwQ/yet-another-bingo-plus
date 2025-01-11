package me.jfenn.bingo.common.card.filter

import kotlinx.serialization.Serializable

@Serializable
class ObjectiveFilterPresets(
    val filters: Map<String, ObjectiveFilterList>
) {
    companion object {
        val EMPTY = ObjectiveFilterPresets(emptyMap())
    }
}
