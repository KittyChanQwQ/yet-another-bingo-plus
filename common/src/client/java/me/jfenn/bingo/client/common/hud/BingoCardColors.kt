package me.jfenn.bingo.client.common.hud

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import me.jfenn.bingo.common.map.Color
import net.minecraft.util.Formatting

@Serializable
data class BingoCardColors(
    val teams: Map<String, Team> = emptyMap(),
) {

    @Serializable
    data class Team(
        val backgroundColor: String = "#ffffffff",
        val textColor: String = "#ff000000",
        val outlineColor: String = "#00000000",
    ) {
        @Transient
        val background = backgroundColor.substring(1)
            .toUInt(16)
            .toInt()
            .let { Color.fromInt(it) }

        @Transient
        val text = textColor.substring(1)
            .toUInt(16)
            .toInt()
            .let { Color.fromInt(it) }

        @Transient
        val outline = outlineColor.substring(1)
            .toUInt(16)
            .toInt()
            .let { Color.fromInt(it) }
    }

    fun getTeam(formatting: Formatting?): Team {
        return teams[formatting?.name?.lowercase() ?: "preview"] ?: Team()
    }
}