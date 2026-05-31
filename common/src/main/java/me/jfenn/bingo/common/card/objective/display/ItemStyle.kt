package me.jfenn.bingo.common.card.objective.display

import kotlinx.serialization.Serializable

@Serializable
enum class ItemStyle {
    NORMAL,
    ONE_OF,
    ALL_OF,
}