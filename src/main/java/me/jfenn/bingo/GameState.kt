package me.jfenn.bingo

enum class GameState(
    val color: String,
    val motd: String,
) {
    PREGAME("§a", "PRE-GAME"),
    PLAYING("§c", "IN-GAME"),
    POSTGAME("§6", "POST-GAME"),
}
