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
        const val IN_THE_END = "in_the_end"
        const val IN_THE_NETHER = "in_the_nether"
        const val ITEM = "item"
        const val ADVANCEMENT = "advancement"
    }
}
