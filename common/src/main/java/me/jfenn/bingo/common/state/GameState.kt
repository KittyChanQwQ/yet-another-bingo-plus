package me.jfenn.bingo.common.state

enum class GameState(
    val color: String,
    val motd: String,
) {
    PREGAME("§a", "PRE-GAME"),
    COUNTDOWN("§c", "IN-GAME"),
    PLAYING("§c", "IN-GAME"),
    POSTGAME("§6", "POST-GAME"),
}
