package me.jfenn.bingo.common.card.objective.display

import kotlinx.serialization.Serializable

@Serializable
enum class Decoration {
    NONE,
    ADVANCEMENT,
    MULTI_ITEM,
    FORBIDDEN,
    FREE_SPACE,
}