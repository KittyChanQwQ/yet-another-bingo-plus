package me.jfenn.bingo.common.state

import me.jfenn.bingo.common.utils.ContextEventListener
import me.jfenn.bingo.common.utils.EventListener

enum class GameState(
    val color: String,
    val motd: String,
) {
    PREGAME("§a", "PRE-GAME"),
    PLAYING("§c", "IN-GAME"),
    POSTGAME("§6", "POST-GAME");

    val onEnter = ContextEventListener<BingoContext, GameState>()

}
