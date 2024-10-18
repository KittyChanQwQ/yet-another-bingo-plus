package me.jfenn.bingo.common.card.filter

sealed class ObjectiveFilter {
    abstract val tag: String
    class Include(override val tag: String): ObjectiveFilter()
    class Exclude(override val tag: String): ObjectiveFilter()
    class Count(override val tag: String, val count: Int): ObjectiveFilter()
}
