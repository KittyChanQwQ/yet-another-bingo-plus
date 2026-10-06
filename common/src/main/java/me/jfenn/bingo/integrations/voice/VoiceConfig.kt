package me.jfenn.bingo.integrations.voice

import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.config.Description

@Serializable
data class VoiceConfig(
    val enabled: Boolean = true,
    @Description("Uses a single combined group when there is only one player per team.")
    val useCombinedGroupForSingleplayerTeams: Boolean = true,
    @Description("Uses a single combined group, regardless of the player/team count")
    val useCombinedGroupAlways: Boolean = false,
    @Description("Group settings used for the per-team groups")
    val teamGroups: VoiceGroupSettings? = VoiceGroupSettings(name = "Bingo: %s Team"),
    @Description("Group settings used for the spectator group")
    val spectatorGroup: VoiceGroupSettings? = VoiceGroupSettings(name = "Bingo: Spectators", type = VoiceGroupType.NORMAL),
    @Description("Group settings used for the combined group")
    val combinedGroup: VoiceGroupSettings? = VoiceGroupSettings(name = "Bingo"),
)
