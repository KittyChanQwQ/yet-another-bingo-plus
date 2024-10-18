package me.jfenn.bingo.common.card.filter

import kotlinx.serialization.Serializable

@Serializable(with = ObjectiveFilterSerializer::class)
class ObjectiveFilterList(
    var list: List<ObjectiveFilter> = emptyList(),
) {

    constructor(vararg filter: ObjectiveFilter) : this(filter.toList())

    fun add(filter: ObjectiveFilter) {
        list = list.filterNot { it.tag == filter.tag }
            .plus(filter)
            .sortedBy { it.tag }
    }

    override fun toString(): String {
        return list.joinToString(" ") {
            when (it) {
                is ObjectiveFilter.Include -> "+${it.tag}"
                is ObjectiveFilter.Exclude -> "-${it.tag}"
                is ObjectiveFilter.Count -> "${it.tag}#${it.count}"
            }
        }
    }

    companion object {
        fun fromString(str: String): ObjectiveFilterList {
            val list = str.split(" ")
                .mapNotNull {
                    when {
                        it.startsWith('+') -> ObjectiveFilter.Include(it.substring(1))
                        it.startsWith('-') -> ObjectiveFilter.Exclude(it.substring(1))
                        it.contains("#") -> {
                            val tag = it.substringBefore('#')
                            val count = it.substringAfter('#').toIntOrNull() ?: 0
                            ObjectiveFilter.Count(tag, count)
                        }
                        else -> null
                    }
                }

            return ObjectiveFilterList(list)
        }
    }
}
