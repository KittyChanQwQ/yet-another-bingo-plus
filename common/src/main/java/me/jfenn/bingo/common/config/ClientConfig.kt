package me.jfenn.bingo.common.config

import kotlinx.serialization.Serializable

@Serializable
data class ClientConfig(
    var cardScale: Float = 1f,
    var cardAlignment: CardAlignment = CardAlignment.TOP_LEFT,
    var cardOffsetX: Int = 0,
    var cardOffsetY: Int = 0,
    var cardOverlap: CardOverlap = CardOverlap.UNDERNEATH,
    var showMultipleCards: Boolean = false,
    var hideOnF3: Boolean = true,
    var hideOnChat: Boolean = false,
)

enum class CardAlignment(val x: Int, val y: Int) {
    TOP_LEFT(0, 0),
    TOP_RIGHT(1, 0),
    BOTTOM_LEFT(0, 1),
    BOTTOM_RIGHT(1, 1),
}

enum class CardOverlap(val z: Int) {
    ABOVE(800),
    UNDERNEATH(-800),
}
