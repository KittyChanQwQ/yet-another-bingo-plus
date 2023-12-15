package me.jfenn.bingo.state

import me.jfenn.bingo.utils.ContextEventListener
import me.jfenn.bingo.utils.EventListener

enum class GameState(
    val color: String,
    val motd: String,
) {
    PREGAME("§a", "PRE-GAME"),
    PLAYING("§c", "IN-GAME"),
    POSTGAME("§6", "POST-GAME");

    val onEnter = ContextEventListener<BingoContext, GameState>()

}
