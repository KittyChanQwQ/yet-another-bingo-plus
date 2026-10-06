package me.jfenn.bingo.common.config

import kotlinx.serialization.Serializable

@Serializable
data class ChatConfig(
    @Description("Whether player chat messages should be sent to their team's chat while in-game.")
    val defaultToTeamChat: Boolean = true,
    @Description("Whether spectating players' chat messages should be sent to an isolated (spectator-only) chat while in-game.")
    val defaultToSpectatorChat: Boolean = true,
    val globalCommandAliases: List<String> = listOf("g", "global", "all"),
    val teamCommandAliases: List<String> = emptyList(),
)