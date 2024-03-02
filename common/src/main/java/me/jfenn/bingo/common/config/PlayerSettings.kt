package me.jfenn.bingo.common.config

import kotlinx.serialization.Serializable

@Serializable
data class PlayerSettings(
    var seenTutorial: Boolean = false,
    var bossbar: Boolean = false,
    var scoreboard: Boolean = true,
    var scoreboardAutoHide: Boolean = false,
    var leadingMessages: Boolean = true,
    var scoreMessages: Boolean = true,
)
