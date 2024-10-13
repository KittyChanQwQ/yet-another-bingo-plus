package me.jfenn.bingo.common.config

import kotlinx.serialization.Serializable
import me.jfenn.bingo.integrations.voice.VoiceGroupSettings
import me.jfenn.bingo.integrations.voice.VoiceGroupType

@Serializable
data class IntegrationsConfig(
    val vanish: Boolean = true,
    val chunky: Boolean = true,
    val chunkyWorlds: Map<String, Int> = mapOf(
        "minecraft:overworld" to 8000,
        "minecraft:the_nether" to 1000,
    ),
    val voice: VoiceConfig = VoiceConfig(),
) {
    @Serializable
    data class VoiceConfig(
        val enabled: Boolean = true,
        val useCombinedGroupForSingleplayerTeams: Boolean = true,
        val useCombinedGroupAlways: Boolean = false,
        val teamGroups: VoiceGroupSettings? = VoiceGroupSettings(name = "Bingo: %s Team"),
        val spectatorGroup: VoiceGroupSettings? = VoiceGroupSettings(name = "Bingo: Spectators", type = VoiceGroupType.NORMAL),
        val combinedGroup: VoiceGroupSettings? = VoiceGroupSettings(name = "Bingo"),
    )
}