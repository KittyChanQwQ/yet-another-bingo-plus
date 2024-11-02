package me.jfenn.bingo.common.card.tag

import kotlinx.serialization.Serializable

@Serializable
class TagData(
    val values: Set<String>
) {
    companion object {
        val EMPTY = TagData(emptySet())
    }

    fun combine(other: TagData): TagData {
        return TagData(
            values = this.values + other.values,
        )
    }

    fun contains(value: String): Boolean {
        return values.contains(value)
    }
}