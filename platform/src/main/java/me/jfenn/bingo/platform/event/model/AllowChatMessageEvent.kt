package me.jfenn.bingo.platform.event.model

import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.commands.ISignedMessage

data class AllowChatMessageEvent(
    val message: ISignedMessage,
    val player: IPlayerHandle,
)
