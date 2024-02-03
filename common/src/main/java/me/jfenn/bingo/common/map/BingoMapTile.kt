package me.jfenn.bingo.common.map

import kotlinx.serialization.Serializable

@Serializable
data class BingoMapTile(
    val isFlashing: Boolean = false,
    val isHidden: Boolean = false,
)
