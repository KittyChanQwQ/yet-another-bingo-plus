package me.jfenn.bingo.common.config

import kotlinx.serialization.Serializable

@Serializable
data class ClientConfig(
    var cardScale: Float = 1f,
    var cardAlignment: CardAlignment = CardAlignment.TOP_LEFT,
    var cardOffsetX: Int = 0,
    var cardOffsetY: Int = 0,
    var showMultipleCards: Boolean = false,
    var hideOnF3: Boolean = true,
    var hideOnChat: Boolean = true,
)

enum class CardAlignment(val x: Int, val y: Int) {
    TOP_LEFT(0, 0),
    TOP_RIGHT(1, 0),
    BOTTOM_LEFT(0, 1),
    BOTTOM_RIGHT(1, 1),
}
