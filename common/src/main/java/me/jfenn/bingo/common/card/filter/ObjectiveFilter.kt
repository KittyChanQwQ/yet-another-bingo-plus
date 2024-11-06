package me.jfenn.bingo.common.card.filter

sealed class ObjectiveFilter {
    abstract val tag: String
    class Include(override val tag: String): ObjectiveFilter()
    class Exclude(override val tag: String): ObjectiveFilter()
    class Count(override val tag: String, val count: Int): ObjectiveFilter()

    companion object {
        const val UNCATEGORIZED = "uncategorized"
        const val UNOBTAINABLE = "unobtainable"
        const val TEDIOUS = "tedious"
        const val EVERYTHING = "everything"
        const val IN_OVERWORLD = "in_overworld"

        fun from(namespace: String) = "from=$namespace"
        fun type(type: String) = "type=$type"
    }
}
