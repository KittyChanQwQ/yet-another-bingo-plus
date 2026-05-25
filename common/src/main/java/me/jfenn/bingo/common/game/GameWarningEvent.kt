package me.jfenn.bingo.common.game

import me.jfenn.bingo.platform.event.IEvent
import me.jfenn.bingo.platform.text.IText

@ConsistentCopyVisibility
data class GameWarningEvent private constructor(
    val warnings: List<IText>,
    val severe: Boolean = false,
) {
    companion object : IEvent<GameWarningEvent> {
        fun info(warnings: List<IText>) = GameWarningEvent(warnings, false)
        fun severe(warnings: List<IText>) = GameWarningEvent(warnings, true)
    }
}
