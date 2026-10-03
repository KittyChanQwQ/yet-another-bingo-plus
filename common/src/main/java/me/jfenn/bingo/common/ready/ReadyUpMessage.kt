package me.jfenn.bingo.common.ready

import me.jfenn.bingo.platform.text.IText

data class ReadyUpMessage(
    val readyUp: IText?,
    val timeRemaining: IText?,
)
