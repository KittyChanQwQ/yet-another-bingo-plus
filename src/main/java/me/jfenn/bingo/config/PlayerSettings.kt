package me.jfenn.bingo.config

import kotlinx.serialization.Serializable

@Serializable
data class PlayerSettings(
    var bossbar: Boolean = true,
    var scoreboard: Boolean = false,
    var leadingMessages: Boolean = true,
    var scoreMessages: Boolean = true,
)
