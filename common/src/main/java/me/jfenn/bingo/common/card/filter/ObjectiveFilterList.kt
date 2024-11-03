package me.jfenn.bingo.common.card.filter

import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.utils.formatTitle
import net.minecraft.text.MutableText
import net.minecraft.text.Text

@Serializable(with = ObjectiveFilterSerializer::class)
data class ObjectiveFilterList(
    val list: List<ObjectiveFilter> = emptyList(),
) : Iterable<ObjectiveFilter> {

    constructor(vararg filter: ObjectiveFilter) : this(filter.toList())

    operator fun plus(filter: ObjectiveFilter): ObjectiveFilterList {
        return this.copy(
            list = list.filterNot { it.tag == filter.tag }
                .plus(filter)
                .sortedBy { it.tag }
        )
    }

    override fun iterator(): Iterator<ObjectiveFilter> {
        return list.iterator()
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
        val DEFAULT = mapOf(
            ObjectiveFilter.EVERYTHING to ObjectiveFilterList(
                ObjectiveFilter.Exclude("unobtainable")
            ),
            ObjectiveFilter.OVERWORLD to ObjectiveFilterList(
                ObjectiveFilter.Include("overworld"),
                ObjectiveFilter.Exclude("uncategorized"),
                ObjectiveFilter.Exclude("unobtainable")
            )
        )

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

        fun formatName(name: String): MutableText = Text.translatableWithFallback("bingo.list.$name", name.formatTitle())
    }
}
