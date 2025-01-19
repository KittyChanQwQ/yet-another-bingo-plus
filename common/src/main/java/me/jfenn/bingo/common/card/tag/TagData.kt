package me.jfenn.bingo.common.card.tag

import kotlinx.serialization.Serializable

@Serializable
class TagData(
    val values: Set<String>
) {
    companion object {
        val EMPTY = TagData(emptySet())
    }

    operator fun plus(other: TagData): TagData {
        return TagData(
            values = this.values + other.values,
        )
    }

    fun contains(value: String): Boolean {
        return values.contains(value)
    }

    operator fun plus(value: String) = TagData(values + value)
    operator fun minus(value: String) = TagData(values - value)

    fun isEmpty() = values.isEmpty()
}