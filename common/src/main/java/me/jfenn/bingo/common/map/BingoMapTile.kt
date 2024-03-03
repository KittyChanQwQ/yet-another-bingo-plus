package me.jfenn.bingo.common.map

import kotlinx.serialization.Serializable

@Serializable
data class BingoMapTile(
    val item: String? = null,
    val isMultiItem: Boolean = false,
    val isFlashing: Boolean = false,
    val isHidden: Boolean = false,
    val isAdvancement: Boolean = false,
)
